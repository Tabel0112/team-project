-- REVIEW FIRST. Not executed by the application. Apply once to a fresh Supabase
-- project after checking existing schema. New uc5_* names deliberately avoid
-- altering any pre-existing study_sets/flashcards tables. Conflicting names fail.
begin;

create table public.uc5_study_sets (
  id uuid primary key,
  user_id uuid not null references auth.users(id),
  name text not null check (length(btrim(name)) > 0),
  revision bigint not null check (revision > 0)
);
create unique index uc5_set_owner_name on public.uc5_study_sets(user_id, lower(name));
create table public.uc5_flashcards (
  id uuid primary key,
  study_set_id uuid not null references public.uc5_study_sets(id) on delete cascade,
  question text not null check (length(btrim(question)) > 0),
  answer text not null check (length(btrim(answer)) > 0),
  position integer not null check (position >= 0),
  unique (study_set_id, position) deferrable initially deferred
);
create index uc5_cards_set on public.uc5_flashcards(study_set_id);
create table public.uc5_save_receipts (
  operation_id uuid primary key,
  user_id uuid not null references auth.users(id),
  study_set_id uuid not null references public.uc5_study_sets(id) on delete cascade,
  payload jsonb not null,
  revision bigint not null
);

alter table public.uc5_study_sets enable row level security;
alter table public.uc5_flashcards enable row level security;
alter table public.uc5_save_receipts enable row level security;
create policy uc5_sets_owner on public.uc5_study_sets for all to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy uc5_cards_owner on public.uc5_flashcards for all to authenticated
  using (exists (select 1 from public.uc5_study_sets s
    where s.id = study_set_id and s.user_id = (select auth.uid())))
  with check (exists (select 1 from public.uc5_study_sets s
    where s.id = study_set_id and s.user_id = (select auth.uid())));
create policy uc5_receipts_owner on public.uc5_save_receipts for all to authenticated
  using ((select auth.uid()) = user_id and exists (select 1 from public.uc5_study_sets s
    where s.id = study_set_id and s.user_id = (select auth.uid())))
  with check ((select auth.uid()) = user_id and exists (select 1 from public.uc5_study_sets s
    where s.id = study_set_id and s.user_id = (select auth.uid())));
revoke all on public.uc5_study_sets, public.uc5_flashcards, public.uc5_save_receipts
  from public, anon, authenticated;
grant select, insert, update, delete on public.uc5_study_sets, public.uc5_flashcards to authenticated;
grant select, insert on public.uc5_save_receipts to authenticated;

-- One PostgREST RPC request is one transaction. SECURITY INVOKER applies RLS.
-- A persisted operation receipt resolves 'committed but response lost' retries.
create function public.uc5_save_set(p_operation_id uuid, p_set_id uuid, p_name text,
    p_expected_revision bigint, p_upserts jsonb, p_deleted_ids jsonb)
returns bigint language plpgsql security invoker set search_path = '' as $$
declare
  actor uuid := auth.uid();
  old_revision bigint;
  new_revision bigint;
  receipt public.uc5_save_receipts%rowtype;
  payload jsonb;
  touched integer;
  card_count integer;
begin
  if actor is null then raise exception 'Authentication required' using errcode = '42501'; end if;
  if p_operation_id is null or p_set_id is null or p_expected_revision < 0
      or p_expected_revision is null or p_name is null or length(btrim(p_name)) = 0
      or jsonb_typeof(p_upserts) is distinct from 'array'
      or jsonb_typeof(p_deleted_ids) is distinct from 'array' then
    raise exception 'Invalid save request' using errcode = '22023';
  end if;
  payload := jsonb_build_object('set_id', p_set_id, 'name', p_name,
    'expected_revision', p_expected_revision, 'upserts', p_upserts, 'deleted_ids', p_deleted_ids);
  -- Also serializes simultaneous first inserts, for which SELECT FOR UPDATE has no row.
  perform pg_advisory_xact_lock(hashtextextended(p_set_id::text, 0));
  select * into receipt from public.uc5_save_receipts
    where operation_id = p_operation_id and user_id = actor;
  if found then
    if receipt.payload <> payload then
      raise exception 'Operation ID reused with different data' using errcode = '22023';
    end if;
    return receipt.revision;
  end if;
  select revision into old_revision from public.uc5_study_sets
    where id = p_set_id and user_id = actor for update;
  if found then
    if old_revision <> p_expected_revision then
      raise exception 'Study set changed since load' using errcode = '40001';
    end if;
    new_revision := old_revision + 1;
    update public.uc5_study_sets set name = p_name, revision = new_revision
      where id = p_set_id and user_id = actor;
  else
    if p_expected_revision <> 0 then
      raise exception 'Study set changed or unavailable' using errcode = '40001';
    end if;
    new_revision := 1;
    insert into public.uc5_study_sets(id, user_id, name, revision)
      values(p_set_id, actor, p_name, new_revision);
  end if;
  if (select count(*) <> count(distinct value->>'id') from jsonb_array_elements(p_upserts))
      or (select count(*) <> count(distinct value) from jsonb_array_elements(p_deleted_ids)) then
    raise exception 'Duplicate card IDs' using errcode = '22023';
  end if;
  if exists(select 1 from jsonb_array_elements(p_upserts) u
      join jsonb_array_elements_text(p_deleted_ids) as d(id) on u->>'id' = d.id) then
    raise exception 'Cannot update and delete the same card' using errcode = '22023';
  end if;
  delete from public.uc5_flashcards where study_set_id = p_set_id
    and id in (select value::uuid from jsonb_array_elements_text(p_deleted_ids));
  get diagnostics touched = row_count;
  if touched <> jsonb_array_length(p_deleted_ids) then
    raise exception 'Deleted card changed or unavailable' using errcode = '40001';
  end if;
  insert into public.uc5_flashcards(id, study_set_id, question, answer, position)
    select (c->>'id')::uuid, p_set_id, c->>'question', c->>'answer', (c->>'position')::integer
    from jsonb_array_elements(p_upserts) c
    on conflict (id) do update set question = excluded.question, answer = excluded.answer,
      position = excluded.position
      where public.uc5_flashcards.study_set_id = p_set_id;
  get diagnostics touched = row_count;
  if touched <> jsonb_array_length(p_upserts) then
    raise exception 'Card does not belong to this set' using errcode = '42501';
  end if;
  select count(*) into card_count from public.uc5_flashcards where study_set_id = p_set_id;
  if exists(select 1 from public.uc5_flashcards where study_set_id = p_set_id and position >= card_count) then
    raise exception 'Card order must be contiguous' using errcode = '22023';
  end if;
  insert into public.uc5_save_receipts(operation_id, user_id, study_set_id, payload, revision)
    values(p_operation_id, actor, p_set_id, payload, new_revision);
  return new_revision;
end;
$$;

-- Single-statement aggregate avoids torn set/card reads and API row-limit truncation.
create function public.uc5_load_set(p_set_id uuid) returns jsonb
language sql stable security invoker set search_path = '' as $$
  select jsonb_build_object('id', s.id, 'name', s.name, 'revision', s.revision,
    'cards', coalesce((select jsonb_agg(jsonb_build_object('id', c.id, 'question', c.question,
      'answer', c.answer, 'position', c.position) order by c.position)
      from public.uc5_flashcards c where c.study_set_id = s.id), '[]'::jsonb))
  from public.uc5_study_sets s where s.id = p_set_id and s.user_id = (select auth.uid());
$$;
revoke all on function public.uc5_save_set(uuid, uuid, text, bigint, jsonb, jsonb) from public, anon;
revoke all on function public.uc5_load_set(uuid) from public, anon;
grant execute on function public.uc5_save_set(uuid, uuid, text, bigint, jsonb, jsonb) to authenticated;
grant execute on function public.uc5_load_set(uuid) to authenticated;
commit;
