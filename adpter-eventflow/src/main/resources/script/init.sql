CREATE TABLE if not exists supos_event_flows (
    id bigint NOT NULL PRIMARY KEY,
    flow_id varchar NULL,
	flow_name varchar Not NULL,
	flow_status varchar Not NULL,
	flow_data TEXT NULL,
	description varchar NULL,
	template varchar null DEFAULT 'node-red',
	create_time timestamptz NULL DEFAULT CURRENT_TIMESTAMP,
	update_time timestamptz NULL
);

CREATE TABLE if not exists supos_event_flow_models (
    parent_id bigint NULL,
	topic varchar NULL,
	create_time timestamptz NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX if not exists  idx_topic ON supos_event_flow_models (topic);