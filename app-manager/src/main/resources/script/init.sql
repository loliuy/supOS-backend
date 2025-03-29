CREATE TABLE  if not exists supos_app_manager (
	name varchar NOT NULL PRIMARY KEY,
	show_name varchar NULL,
	description varchar NULL,
	create_time timestamptz DEFAULT now() NULL
);

CREATE TABLE  if not exists supos_app_html (
	id bigint NOT NULL PRIMARY KEY,
	app_name varchar NOT NULL,
	path varchar NOT NULL,
	homepage int2 NULL DEFAULT 0,
	file_name varchar NOT NULL,
	create_time timestamptz DEFAULT now() NULL
);
CREATE INDEX if not exists supos_app_html_app_name_idx ON supos_app_html (app_name);
