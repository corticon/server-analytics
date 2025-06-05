/******************************************************************************************************/
USE [master]
GO
DROP TABLE [dbo].[CORTICON_ANALYTICS_RULE_AUDITING]
GO
DROP TABLE [dbo].[CORTICON_ANALYTICS_RULE_EXECUTION]
GO
DROP TABLE [dbo].[CORTICON_ANALYTICS_RULE_MESSAGES]
GO
DROP TABLE [dbo].[CORTICON_ANALYTICS_PAYLOADS]
GO
DROP TABLE [dbo].[CORTICON_ANALYTICS_RULE_CATALOG]
GO
DROP TABLE [dbo].[CORTICON_ANALYTICS_EXECUTION]
GO
DROP TABLE [dbo].[CORTICON_ANALYTICS_DECISION_SERVICE]
GO


/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/****** Object:  Table [dbo].[CORTICON_ANALYTICS_DECISION_SERVICE]    Script Date: 7/31/2023 7:39:24 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[CORTICON_ANALYTICS_DECISION_SERVICE](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [varchar](300) NULL,
	[major_version_number] [int] NULL,
	[minor_version_number] [int] NULL,
 CONSTRAINT [PK_CORTICON_ANALYTICS_DECISION_SERVICE] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
CREATE TABLE [dbo].[CORTICON_ANALYTICS_EXECUTION](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[decision_service_id] [int] NULL,
	[execution_timestamp] [datetime] NULL,
	[total_time] [int] NULL,
 CONSTRAINT [PK_CORTICON_ANALYTICS_EXECUTION] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/****** Object:  Table [dbo].[CORTICON_ANALYTICS_PAYLOADS]    Script Date: 7/31/2023 7:42:19 PM ******/
CREATE TABLE [dbo].[CORTICON_ANALYTICS_PAYLOADS](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[execution_id] [int] NULL,
	[input] [varchar](max) NULL,
	[output] [varchar](max) NULL,
 CONSTRAINT [PK_CORTICON_ANALYTICS_PAYLOADS] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
CREATE TABLE [dbo].[CORTICON_ANALYTICS_RULE_MESSAGES](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[execution_id] [int] NULL,
	[severity] [varchar](100) NULL,
	[text] [varchar](max) NULL,
	[entity_reference] [varchar](100) NULL,
 CONSTRAINT [PK_CORTICON_ANALYTICS_RULE_MESSAGES] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO

/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
CREATE TABLE [dbo].[CORTICON_ANALYTICS_RULE_AUDITING](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[execution_id] [int] NULL,
	[rule_catalog_id] [int] NULL,
	[sequence_number] [int] NULL,
	[action] [varchar](100) NULL,
	[entity_name] [varchar](100) NULL,
	[entity_identity_values] [varchar](100) NULL,
	[attribute_name] [varchar](100) NULL,
	[attribute_old_value] [varchar](500) NULL,
	[attribute_new_value] [varchar](500) NULL,
	[entity_role_name] [varchar](100) NULL,
	[association_entity_name] [varchar](100) NULL,
	[association_entity_identity_values] [varchar](100) NULL,
 CONSTRAINT [PK_CORTICON_ANALYTICS_RULE_AUDITING] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
CREATE TABLE [dbo].[CORTICON_ANALYTICS_RULE_CATALOG](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[decision_service_id] [int] NULL,
	[rulesheet_name] [varchar](500) NULL,
	[rule_number] [varchar](50) NULL,
 CONSTRAINT [PK_CORTICON_ANALYTICS_RULE_CATALOG] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
CREATE TABLE [dbo].[CORTICON_ANALYTICS_RULE_EXECUTION](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[execution_id] [int] NULL,
	[rule_catalog_id] [int] NULL,
	[count] [int] NULL,
 CONSTRAINT [PK_CORTICON_ANALYTICS_RULE_EXECUTION] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
/******************************************************************************************************/
ALTER TABLE [dbo].[CORTICON_ANALYTICS_EXECUTION]  WITH CHECK ADD  CONSTRAINT [FK_CORTICON_ANALYTICS_EXECUTION_DECISION_SERVICE] FOREIGN KEY([decision_service_id])
REFERENCES [dbo].[CORTICON_ANALYTICS_DECISION_SERVICE] ([id])
GO

ALTER TABLE [dbo].[CORTICON_ANALYTICS_EXECUTION] CHECK CONSTRAINT [FK_CORTICON_ANALYTICS_EXECUTION_DECISION_SERVICE]
GO
/******************************************************************************************************/
ALTER TABLE [dbo].[CORTICON_ANALYTICS_PAYLOADS]  WITH CHECK ADD  CONSTRAINT [FK_CORTICON_ANALYTICS_PAYLOADS_EXECUTION] FOREIGN KEY([execution_id])
REFERENCES [dbo].[CORTICON_ANALYTICS_EXECUTION] ([id])
GO

ALTER TABLE [dbo].[CORTICON_ANALYTICS_PAYLOADS] CHECK CONSTRAINT [FK_CORTICON_ANALYTICS_PAYLOADS_EXECUTION]
GO
/******************************************************************************************************/
ALTER TABLE [dbo].[CORTICON_ANALYTICS_RULE_AUDITING]  WITH CHECK ADD  CONSTRAINT [FK_CORTICON_ANALYTICS_RULE_AUDITING_EXECUTION] FOREIGN KEY([execution_id])
REFERENCES [dbo].[CORTICON_ANALYTICS_EXECUTION] ([id])
GO

ALTER TABLE [dbo].[CORTICON_ANALYTICS_RULE_AUDITING] CHECK CONSTRAINT [FK_CORTICON_ANALYTICS_RULE_AUDITING_EXECUTION]
GO

/******************************************************************************************************/
ALTER TABLE [dbo].[CORTICON_ANALYTICS_RULE_CATALOG]  WITH CHECK ADD  CONSTRAINT [FK_CORTICON_ANALYTICS_RULE_CATALOG_DECISION_SERVICE] FOREIGN KEY([decision_service_id])
REFERENCES [dbo].[CORTICON_ANALYTICS_DECISION_SERVICE] ([id])
GO

ALTER TABLE [dbo].[CORTICON_ANALYTICS_RULE_CATALOG] CHECK CONSTRAINT [FK_CORTICON_ANALYTICS_RULE_CATALOG_DECISION_SERVICE]
GO
/******************************************************************************************************/
ALTER TABLE [dbo].[CORTICON_ANALYTICS_RULE_EXECUTION]  WITH CHECK ADD  CONSTRAINT [FK_CORTICON_ANALYTICS_RULE_EXECUTION_EXECUTION] FOREIGN KEY([execution_id])
REFERENCES [dbo].[CORTICON_ANALYTICS_EXECUTION] ([id])
GO

ALTER TABLE [dbo].[CORTICON_ANALYTICS_RULE_EXECUTION] CHECK CONSTRAINT [FK_CORTICON_ANALYTICS_RULE_EXECUTION_EXECUTION]
GO

/******************************************************************************************************/
ALTER TABLE [dbo].[CORTICON_ANALYTICS_RULE_MESSAGES]  WITH CHECK ADD  CONSTRAINT [FK_CORTICON_ANALYTICS_RULE_MESSAGES_CORTICON_ANALYTICS_EXECUTION] FOREIGN KEY([execution_id])
REFERENCES [dbo].[CORTICON_ANALYTICS_EXECUTION] ([id])
GO

ALTER TABLE [dbo].[CORTICON_ANALYTICS_RULE_MESSAGES] CHECK CONSTRAINT [FK_CORTICON_ANALYTICS_RULE_MESSAGES_CORTICON_ANALYTICS_EXECUTION]
GO
/******************************************************************************************************/


