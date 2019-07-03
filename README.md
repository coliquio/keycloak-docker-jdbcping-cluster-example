# Keycloak Cluster based on Docker + JDBC_PING

Note: this demonstrates issues persisting sessions using Keycloak Infinispan JDBC Store

## Start the cluster

```bash
# just start
docker-compose -f docker-compose.yml -f docker-compose.ports.yml up

# restart and rebuild
docker-compose down && docker-compose -f docker-compose.yml -f docker-compose.ports.yml up --build
```

## Create Sessions

### Cluster

```bash
open http://localhost:8000/auth/realms/example/account

# Username `admin`
# Password `admin`
```

### Single Node

```bash
open http://localhost:8081/auth/realms/example/account

# Username `admin`
# Password `admin`

# => works, the "Edit Account" view is shown
```

### Stopping/starting nodes and testing session aliveness

1. Stop instance 1 `docker stop keycloak-docker-jdbcping-cluster-example_mysql_jdbcping_1`
2. Check in browser if you are still logged in -> works, because sessions are spread in cluster memory (CORRECT)
3. Stop instance 2 `docker stop keycloak-docker-jdbcping-cluster-example_mysql_jdbcping_2`
4. Of course, now no web UI is available
5. Start instance 1 again `docker start keycloak-docker-jdbcping-cluster-example_mysql_jdbcping_1`
6. Check in browser if you are still logged in -> works, because sessions were not persisted in mysql db (WRONG)

## Analyse Internals

### JDBC Sessions

No client sessions (does not match expectations because user just logged in)

```bash
mysql --host 127.0.0.1 --user root --password=root --database keycloak --execute "select * from ISPN_clientSessions;"
```

No sessions (does not match expectations because user just logged in)

```bash
mysql --host 127.0.0.1 --user root --password=root --database keycloak --execute "select * from ISPN_sessions;"
```

### Container Logs

```bash
[...]
keycloak1_1       | 15:12:34,152 INFO  [org.infinispan.CLUSTER] (remote-thread--p8-t5) [Context=work] ISPN100010: Finished rebalance with members [keycloak1, keycloak2], topology id 5
keycloak1_1       | 15:12:34,152 INFO  [org.infinispan.CLUSTER] (remote-thread--p8-t1) [Context=clientSessions] ISPN100010: Finished rebalance with members [keycloak1, keycloak2], topology id 5
keycloak1_1       | 15:12:34,164 INFO  [org.infinispan.CLUSTER] (remote-thread--p8-t7) [Context=authenticationSessions] ISPN100010: Finished rebalance with members [keycloak1, keycloak2], topology id 5
[...]
```

### JDBC_PING MySQL Status

```bash
$ mysql --host 127.0.0.1 --user root --password=root --database keycloak --execute "select * from JGROUPSPING;"

mysql: [Warning] Using a password on the command line interface can be insecure.
+--------------------------------------+-----------+---------------------+--------------+------------------------------------------+
| own_addr                             | bind_addr | created             | cluster_name | ping_data                                |
+--------------------------------------+-----------+---------------------+--------------+------------------------------------------+
| 6d69d5f7-4c84-18cb-6d64-dd2ddb2b3c99 | keycloak1 | 2019-03-21 15:12:31 | ejb          | md�-�+<�mi��L�� 	keycloak1� ���            |
| f976f2e6-c4f5-ada4-796f-99f1c3b97193 | keycloak1 | 2019-03-21 15:12:31 | ejb          | yo��ùq��v���� 	keycloak2� ���                |
+--------------------------------------+-----------+---------------------+--------------+------------------------------------------+
```

### Health Check Module

```javascript
$ curl http://localhost:8081/auth/realms/master/health/check
{
  "details": {
    "infinispan": {
      "numberOfNodes": 2,
      "state": "UP",
      "healthStatus": "HEALTHY",
      "nodeNames": [
        "keycloak1",
        "keycloak2"
      ],
      "cacheDetails": [
        {
          "cacheName": "realms",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "authenticationSessions",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "sessions",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "authorizationRevisions",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "work",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "keys",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "clientSessions",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "users",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "loginFailures",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "offlineClientSessions",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "authorization",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "realmRevisions",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "offlineSessions",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "actionTokens",
          "healthStatus": "HEALTHY"
        },
        {
          "cacheName": "userRevisions",
          "healthStatus": "HEALTHY"
        }
      ],
      "clusterName": "ejb"
    },
    "database": {
      "connection": "established",
      "state": "UP"
    },
    "filesystem": {
      "freebytes": 13042221056,
      "state": "UP"
    }
  },
  "name": "keycloak",
  "state": "UP"
}
```

## Resources

- https://www.keycloak.org/docs/latest/server_installation/index.html#_clustering
