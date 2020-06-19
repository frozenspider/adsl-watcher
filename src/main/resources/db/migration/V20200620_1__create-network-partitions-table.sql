CREATE TABLE `network_partitions` (
	`id` INT NOT NULL AUTO_INCREMENT,
	`start_time` TIMESTAMP NOT NULL,
	`end_time` TIMESTAMP NOT NULL,
	`duration` INT NOT NULL COMMENT 'In seconds',
	`message` TEXT NULL,
	PRIMARY KEY (`id`)
)
COLLATE='utf8_unicode_ci'
;
