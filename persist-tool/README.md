## Overview

The bal persist feature allows you to store data in different data stores and retrieve them when needed. A data store can be a database or an in-memory cache. The bal persist feature currently supports in-memory tables, SQL databases, Google Sheets, and Redis as data stores. As you can use the same syntax to access data in all these data stores, you do not need to learn different syntaxes to access data in different data stores.

The bal persist CLI tool is used to initialize the project with the bal persist command and generate the required files.

There are two ways that you can use the bal persist feature.

1. Integrate the client code generation with the package build.
2. One-time generation of the client code against the data model.

## Integrate the client code generation with the package build

The Ballerina project should be initialized with `bal persist` before generating the derived types, clients, and script files. This can be done using the `add` command. You can specify the preferred data store and the module, which you need to generate files. If you do not specify the data store and the module, the default values will be used.

```
$ bal persist add --datastore mysql --module store --test-datastore h2
```

|  Command option  |                                                                         Description                                                                          | Mandatory  |  Default value  |
|:----------------:|:------------------------------------------------------------------------------------------------------------------------------------------------------------:|:----------:|:---------------:|
|   --datastore    |   Used to indicate the preferred database client. Currently, `inmemory`, `mysql`, `mssql`, `postgresql`, `h2`, `googlesheets`, and  `redis` are supported.   |     No     |   `inmemory`    |
|     --module     |                                        Used to indicate the persist-enabled module in which the files are generated.                                         |     No     | `<root_module>` |
| --test-datastore | Used to indicate the preferred data store for the test cases. It can be either `inmemory` for non-SQL or `h2` for SQL, as these are the supported datastore.  |     No     |       No        |

The command initializes the `bal persist` feature in the project. This command will do the following,

1. Create the `persist` directory in the project root directory.
   This directory contains the data model definition file (`model.bal`) of the project.

2. Create a model definition file in the `persist` directory.
   This file is used to model the data, which need to be persisted in the project. You can have only one data model definition file in the project and the file name can be anything with the `.bal` extension. The default file name is `model.bal`. Entities defined in this file should be based on the [`persist model` specification](/learn/persist-model).

3. Update the `Ballerina.toml` file with the `persist` module configurations.
   It will update the `Ballerina.toml` file with `persist` configurations as follows.
    ```ballerina
   [[tool.persist]]
   id = "generate-db-client"
   targetModule = "store"
   options.datastore = "mysql"
   options.testDatastore = "h2"
   filePath = "persist/model.bal"
   ```

The directory structure will be,
```
rainier
   ├── persist
            └── model.bal
   ├── Ballerina.toml
   └── main.bal
```

Behaviour of the `add` command,
- You should invoke the command from within a Ballerina project.
- You can use the optional arguments to indicate the preferred module name and data store. Otherwise, the default values will be used.
- You can use test-datastore to indicate the preferred database client for testing. It can be either `inmemory` for non-SQL or `h2` for SQL, as these are the supported datastore. If specified it will update the Ballerina.toml file with the testDatastore option to generate the test client along with the main client at the build time.
- You cannot execute the command multiple times within the same project. You need to remove the `Ballerina.toml` configurations if the user wants to reinitialize the project.

## One-time generation of the client code against the data model

### Initialize `bal persist` in the project

you can initialize the project with the following `init` command,

```
bal persist init
```

This command includes the following steps,

1. Create persist directory:
   Within this directory, a data model definition file should be created. This file will outline the necessary entities according to the [`persist` specification](https://github.com/ballerina-platform/module-ballerina-persist/blob/main/docs/spec/spec.md#2-data-model-definition)
2. Generate a model definition file within the persist directory:
   This action will create a file named model.bal with the requisite imports (import ballerina/persist as _;) if no files currently exist in the persist directory.

### Generate the derived types, client, and script files

You can use the `bal persist generate` command to generate the derived types, client, and script files.

```
bal persist generate --datastore mysql --module store --test-datastore h2
```

|  Command option  |                                                                         Description                                                                         | Mandatory | Default Value  |
|:----------------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------:|:---------:|:--------------:|
|   --datastore    |  Used to indicate the preferred database client. Currently, `inmemory`, `mysql`, `mssql`, `postgresql`, `h2`, `googlesheets`, and  `redis` are supported.   |    Yes     |                |
|     --module     |                                        Used to indicate the persist enabled module in which the files are generated.                                        |    No     | <package_name> |
| --test-datastore | Used to indicate the preferred data store for the test cases. It can be either `inmemory` for non-SQL or `h2` for SQL, as these are the supported datastore. |     No     |       No        |

If the module name is provided, it will generate the files under a new subdirectory with the module name like below. Otherwise, it will generate the files under the `root` directory.

```
rainier
   ├── modules
        └── store
              ├── persist_client.bal
              ├── persist_db_config.bal
              ├── persist_types.bal
              ├── persist_test_client.bal (This file will be generated if the test-datastore is provided)
              ├── persist_test_init.bal (This file will be generated if h2 is provided as the test-datastore)
              └── script.sql
   ├── persist
        └── model.bal
   ├── Ballerina.toml
   ├── Config.toml
   └── main.bal
```

Behaviour of the `generate` command,
- User should invoke the command within a Ballerina project
- The model definition file should contain the `persist` module import (`import ballerina/persist as _;`)
- The model definition file should contain at least one entity
- If the user invokes the command twice, it will fail if the client's native dependency exists in the `Ballerina.toml` file and is different from the current version. The user should remove the persist native dependency from the `Ballerina.toml` configurations to generate the files again.

## Generate the data model by introspecting an existing database

The command below generates the data model for an existing database. The generated data model can be used to generate the client API for the existing database after verifying it.

```
$ bal persist pull --datastore mysql --host localhost --port 3306 --user root --database db
```

| Command option |                                               Description                                               | Mandatory | Default Value |
|:--------------:|:-------------------------------------------------------------------------------------------------------:|:---------:|:-------------:|
|  --datastore   | Used to indicate the preferred data store. Currently, `mysql`, `mssql`, and `postgresql` are supported. |    No     |     mysql     |
|     --host     |                                   Used to indicate the database host                                    |    Yes    |     None      |
|     --port     |                                   Used to indicate the database port                                    |    No     |     3306      |
|     --user     |                                   Used to indicate the database user                                    |    Yes    |     None      |
|   --database   |                                   Used to indicate the database name                                    |    Yes    |     None      |

The file structure of the project after executing the command will be as follows.

```
rainier
   ├── persist
         └── model.bal   
   ├── Ballerina.toml
   ├── Config.toml
   └── main.bal
```

This command introspects the schema of the database and creates a `model.bal` file with the entities and relations based on the schema of the database. The database configuration should be provided as command-line arguments.

The `persist` directory is created if it is not already present. If a `model.bal` file is already present in the `persist` directory, it will prompt the user to confirm overwriting the existing `model.bal` file.

Running the `pull` command will,
1. Create a `model.bal` file with the entities and relations based on the introspected schema of the database.
2. Not change the schema of the database in any way.

The behaviour of the `pull` command is as follows
- User should invoke the command within a Ballerina project.
- User should provide the relevant database configuration as command-line arguments.
- The database password is not provided as a command-line argument. The user will be prompted to enter the password.
- If the user invokes the command while a `model.bal` file exists in the `persist` directory, it will prompt the user to confirm overwriting the existing `model.bal` file.
- If the user introspects a database with unsupported data types, it will inform the user by giving a warning and will comment out the relevant field with the tag `[Unsupported[DATA_TYPE]]`.
- The user must execute the `generate` command to generate the derived types and client API after running the `pull` command in order to use the client API in the project.

## Generate migration scripts for the model definition changes [Experimental]

>**Info:** The support for migrations is currently an experimental feature, and its behavior may be subject to change in future releases. Also, the support for migrations is currently limited to the MySQL data store.

The command below generates the migration scripts for the model definition changes. This command is executed in the project root directory. This command will generate the migration scripts based on the changes in the model definition file. The generated migration scripts will be added to the `migrations` directory inside the `persist` directory.

```
$ bal persist migrate add_employee
```

The file structure of the project after executing the command will be as follows.

```
rainier
   ├── generated
         └── store
               ├── persist_client.bal
               ├── persist_db_config.bal
               ├── persist_types.bal
               └── script.sql
   ├── persist
         └── model.bal
         └── migrations
               └── 20200820120000_add_employee
                  ├── model.bal
                  └── script.sql     
   ├── Ballerina.toml
   ├── Config.toml
   └── main.bal
```

This command will create a new directory for the migration with the timestamp and the label provided by the user. The directory will contain the model definition file and the script file. The script file will contain the SQL script to update the database schema. The model definition file will be the snapshot of the model definition file at the time of executing the command. The next time you execute the command, it will compare the model definition file with the latest snapshot file and generate the SQL script to update the database schema. Therefore, you should not modify the snapshot file.

The behaviour of the `migrate` command will be as follows.
- You should invoke the command within a Ballerina project.
- You should provide a valid label for the migration.
- You should have `bal persist` initiated in the project and the model definition file updated.
- You can execute the command multiple times. It will generate the migration scripts based on the changes in the model definition file.
- Once the migration is generated, you cannot change the data store type in the `Ballerina.toml` file. If you want to change the data store type, you need to remove the migration directory and reinitialize the project.
