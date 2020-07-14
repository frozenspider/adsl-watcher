ALTER TABLE `network_partitions`
	CHANGE COLUMN `end_time` `end_time` TIMESTAMP NULL AFTER `start_time`,
	CHANGE COLUMN `duration` `duration` INT NULL COMMENT 'In seconds' AFTER `end_time`;
