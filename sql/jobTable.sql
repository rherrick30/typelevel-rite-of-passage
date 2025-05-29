CREATE TABLE IF NOT EXISTS public.jobs (
id uuid DEFAULT gen_random_uuid(),
date bigint not null,
ownerEmail text not null,
company text not null,
title text not null,
description text not null,
externalUrl text not null,
remote boolean not null default false,
location text not null,
salaryLo integer,
salaryHi integer,
currency text,
country text,
tags text[],
image text,
seniority text,
other text,
active boolean not null default false
);

alter table jobs add constraint pk_jobs PRIMARY KEY(id);