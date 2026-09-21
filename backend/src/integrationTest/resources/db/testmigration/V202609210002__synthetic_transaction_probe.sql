-- Integration-test-only table, never packaged in the application.
CREATE TABLE public.synthetic_probe (id bigint PRIMARY KEY, label varchar(255) NOT NULL);
