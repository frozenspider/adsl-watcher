ALTER TABLE `router_info_records`
    CHANGE COLUMN `snr_margin` `downstream_snr_margin` DOUBLE NULL DEFAULT NULL AFTER `annex_mode`,
    CHANGE COLUMN `line_attenuation` `downstream_line_attenuation` DOUBLE NULL DEFAULT NULL AFTER `downstream_snr_margin`,
    CHANGE COLUMN `line_rate` `downstream_data_rate` INT(11) NULL DEFAULT NULL AFTER `downstream_line_attenuation`,
    CHANGE COLUMN `crc_errors` `downstream_crc_errors` INT(11) NULL DEFAULT NULL AFTER `downstream_data_rate`,
    CHANGE COLUMN `errored_seconds` `downstream_errored_seconds` INT(11) NULL DEFAULT NULL AFTER `downstream_crc_errors`,
    CHANGE COLUMN `severely_errored_seconds` `downstream_severely_errored_seconds` INT(11) NULL DEFAULT NULL AFTER `downstream_errored_seconds`,
    ADD COLUMN `upstream_snr_margin` DOUBLE NULL DEFAULT NULL AFTER `downstream_severely_errored_seconds`,
    ADD COLUMN `upstream_line_attenuation` DOUBLE NULL DEFAULT NULL AFTER `upstream_snr_margin`,
    ADD COLUMN `upstream_data_rate` INT(11) NULL DEFAULT NULL AFTER `upstream_line_attenuation`,
    ADD COLUMN `upstream_crc_errors` INT(11) NULL DEFAULT NULL AFTER `upstream_data_rate`,
    ADD COLUMN `upstream_errored_seconds` INT(11) NULL DEFAULT NULL AFTER `upstream_crc_errors`,
    ADD COLUMN `upstream_severely_errored_seconds` INT(11) NULL DEFAULT NULL AFTER `upstream_errored_seconds`;
