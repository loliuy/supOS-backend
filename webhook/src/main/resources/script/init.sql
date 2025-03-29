CREATE TABLE if not exists supos_webhook (
    id bigint NOT NULL PRIMARY KEY,
    name varchar Not NULL,
    subscribe_event varchar NULL,
	url varchar Not NULL,
	headers varchar NULL,
	status int2 NOT NULL DEFAULT 1,
	description varchar NULL,
	create_time timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
	update_time timestamptz NULL
);

INSERT INTO supos.supos_webhook(id, "name", subscribe_event, url, headers, status, description)
VALUES(1, 'fuxa', 'INSTANCE_FIELD_CHANGE', 'http://fuxa:1881/api/systemDevices', NULL, 1, NULL) ON CONFLICT (id) do nothing;
INSERT INTO supos.supos_webhook(id, "name", subscribe_event, url, headers, status, description)
VALUES(2, 'fuxa-delete', 'INSTANCE_DELETE', 'http://fuxa:1881/api/deleteSystemDevices', NULL, 1, NULL) ON CONFLICT (id) do nothing;