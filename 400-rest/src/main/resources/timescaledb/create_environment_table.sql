-- Copyright 2022 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

BEGIN;

CREATE TABLE IF NOT EXISTS public.cg_environments (
    id text NOT NULL,
    name text NOT NULL,
    account_id text,
    env_type text,
    app_id text NOT NULL,
    created_at bigint,
    last_updated_at bigint,
    created_by text,
    last_updated_by text,

    PRIMARY KEY(id)
);

COMMIT;
