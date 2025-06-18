-- AUTO-GENERATED FILE.

-- This file is an auto-generated file by Ballerina persistence layer for model.
-- Please verify the generated scripts and execute them against the target DB server.

DROP TABLE IF EXISTS `MedicalItem`;
DROP TABLE IF EXISTS `MedicalNeed`;

CREATE TABLE `MedicalNeed` (
	`needId` INT NOT NULL,
	`itemId` INT NOT NULL,
	`name` VARCHAR(191) NOT NULL,
	`beneficiaryId` INT NOT NULL,
	`period` DATETIME NOT NULL,
	`urgency` VARCHAR(191) NOT NULL,
	`quantity` VARCHAR(191) NOT NULL,
	PRIMARY KEY(`needId`)
);

CREATE TABLE `MedicalItem` (
	`itemId` INT NOT NULL,
	`name` VARCHAR(191) NOT NULL,
	`type` VARCHAR(191) NOT NULL,
	`unit` INT NOT NULL,
	PRIMARY KEY(`itemId`)
);


