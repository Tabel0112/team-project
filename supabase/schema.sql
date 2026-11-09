-- Run once in the Supabase SQL editor. Use only the public anon/publishable key in the app.
create table if not exists public.study_sets (
    user_id uuid not null references auth.users(id) on delete cascade,
    id uuid not null,
    data jsonb not null check (jsonb_typeof(data) = 'object'),
    primary key (user_id, id)
);
alter table public.study_sets enable row level security;
revoke all on public.study_sets from anon;
grant select, insert, update, delete on public.study_sets to authenticated;
drop policy if exists "Users manage their own study sets" on public.study_sets;
create policy "Users manage their own study sets"
    on public.study_sets for all to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);
