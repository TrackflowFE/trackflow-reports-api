CREATE TABLE IF NOT EXISTS reports_shipment_tracking (
    tracking_number varchar(255) PRIMARY KEY,
    status varchar(255) NOT NULL,
    sender_name varchar(255) NOT NULL,
    origin_city_id bigint NOT NULL,
    origin_city varchar(255) NOT NULL,
    recipient_name varchar(255) NOT NULL,
    recipient_address varchar(255) NOT NULL,
    destination_city_id bigint NOT NULL,
    destination_city varchar(255) NOT NULL,
    registered_at timestamp with time zone NOT NULL,
    last_movement_point varchar(255),
    last_movement_at timestamp with time zone,
    last_movement_city_id bigint,
    last_movement_city varchar(255),
    next_stop_city varchar(255),
    planned_leg_hours integer
);
CREATE TABLE IF NOT EXISTS reports_shipment_transit (
    tracking_number varchar(255) PRIMARY KEY,
    received_at timestamp with time zone,
    dispatched_at timestamp with time zone,
    arrived_at timestamp with time zone,
    out_for_delivery_at timestamp with time zone,
    delivered_at timestamp with time zone
);
CREATE TABLE IF NOT EXISTS reports_shipment_volume (
    tracking_number varchar(255) PRIMARY KEY,
    registered_at timestamp with time zone NOT NULL,
    origin_city_id bigint NOT NULL,
    origin_city varchar(255) NOT NULL,
    entry_center_id bigint,
    entry_point varchar(255),
    entry_city_id bigint,
    entry_city varchar(255),
    received_at timestamp with time zone,
    delivered_at timestamp with time zone
);
