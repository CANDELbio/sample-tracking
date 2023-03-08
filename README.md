# sample-tracking

## Admin Console Documentation

Instructions for the usage of the admin console can be found on the [eReq Admin Console page](docs/console.md).

## Tech Stack

Server side [ring](https://github.com/ring-clojure/ring) app:

* Static asset serving
* JSON REST API with echo endpoint for prototyping
* Hot code reloading

Client side [re-frame](https://github.com/Day8/re-frame) app:

* Routing using [bidi](https://github.com/juxt/bidi) and [pushy](https://github.com/clj-commons/pushy)
* Event interceptor that validates app DB against spec in development
* Components structured into separate namespaces each with their own db spec, event handlers, subscriptions, and views.
  Namespaced using the
  re-frame [synthetic namespace](https://github.com/Day8/re-frame/blob/master/docs/Namespaced-Keywords.md) pattern
* Pages separated out with potential to use parallel structure to components as their complexity grows

To demonstrate things a homepage with a sign up form that POSTs to an API which just echo's back the response is
provided.

## Development Mode

Start figwheel-main:

```bash
$ lein fig:build
```

This should start a ring server and automatically building your application. Once it's ready, it should open a new
browser window with the application for you.

## Node packages and interop

Setup using this guide: [Figwheel-main and NPM Modules](https://figwheel.org/docs/npm.html)

To add new modules, add them to npm

```bash
$ npm install --save <package>
```

And then import the package add it to the window in src/js/index.js. Before starting the webserver, run the following
commands to update the bundle of external modules:

```bash
$ npm install
$ npx webpack --mode=development
```

## Creating and serving the database

Download Datomic Pro and run the following commands from within the unzipped folder.

Start the transactor with your license in the properties in one terminal.

```bash
./bin/transactor ../dev-transactor.properties
```

In s separate terminal, start the repl and delete any existing databases and recreate them. You can skip the
delete-database step if this is your first time creating them.

```bash
./bin/repl
Clojure 1.10.1
```

```clojure
user=> (require 'datomic.api)
nil
user=> (datomic.api/delete-database "datomic:dev://localhost:4334/ereq-dev")
true
user=> (datomic.api/create-database "datomic:dev://localhost:4334/ereq-dev")
true
user=> (datomic.api/delete-database "datomic:dev://localhost:4334/ereq-test")
true
user=> (datomic.api/create-database "datomic:dev://localhost:4334/ereq-test")
true
```

Start serving the databases in separate terminals.

```bash
./bin/run -m datomic.peer-server -h localhost -p 8998 -a myaccesskey,mysecret -d ereq-dev,datomic:dev://localhost:4334/ereq-dev
```

```bash
./bin/run -m datomic.peer-server -h localhost -p 9119 -a myaccesskey,mysecret -d ereq-test,datomic:dev://localhost:4334/ereq-test
```

## Populating the database

### Initializing the database

You can run the following command to transact the schema and add initial form data:

```bash
lein run test-setup
```

### Editing and transacting the schema

This project uses Datomic as its database. Datomic configuration defaults are stored in `resources/config/datomic.edn`,
and can be overridden by environment variables defined in the same file. The database schema is stored
in `src/clj/org/parkerici/sample_tracking/db/schema.clj`.

If you make changes to the schema, run `lein run transact-schema` to generate a new Datomic schema file
at `resources/schema.edn` and to transact the changes to the configured database.

### Populating roles

To add new roles from the configuration files to the database run the following command. You only need to do this if you
are adding new roles.

```bash
lein run create-roles
```

### Adding admin users

To add an admin user from the CLI run the following command.

```bash
lein run add-admin user@gsuite.com
```

## Running Tests

The test database ereq-test must be running for tests to run successfully.

Before running tests for the first time you must populate the test database.

```bash
lein with-profile test run test-setup
```

Sometimes with-profile doesn't work. If this is the case you can manually set the environment variables.

```bash
export DATOMIC_ENDPOINT=localhost:9119
export DATOMIC_DB_NAME=ereq-test
export SEND_MANIFEST_EMAILS=false
```

Once you've done this you can run the tests with the following command.

```bash
lein test
```

## Creating new Forms

The process for creating new forms can be found [here](docs/forms.md).

## Deploying on GCS

### Create a k8s cluster

Make sure it's VPC native

### Create a Postgres database

https://console.cloud.google.com/sql/instances

Record name and password:  sample-tracking / <pwd>

This can take over a half hour to complete....but you can get IP address first

Give it a Private IP address Use Connections Tab, Turn on the required API. etc

### Create Datomic DB

See https://docs.datomic.com/on-prem/storage.html

Connect to the database:

```bash
$ gcloud sql connect <cloudsql-db-name> --user=postgres
```

Copy and paste the Postgres datomic `postgres-db.sql` scripts into the prompt. You have to delete the TABLESPACE
argument from the db creation script.

After you create the database, connect to it by running \c datomic in the psql command line.

Next, run the `postgres-table.sql` and the `postgres-user.sql` in the `datomic` db.

### Create App DB

[ don՚t do this if you are restoring from backup! ]

Setup and create the transactor pod:

```bash
$ kubectl --namespace=default create secret generic datomic-transactor-properties --from-file=transactor.properties=./secrets/transactor.properties
$ kubectl apply -f ./deploy/k8s/datomic/transactor.yaml
```

Attach to the pod and create the DB in Datomic. Make sure to substitute the IP of the postgres instance – it should be
same as in transactor.properties.

```bash
$ kubectl get pods
$ kubectl exec -it $(kubectl get pods --selector=app=datomic-transactor -o jsonpath={.items..metadata.name}) -- /bin/bash
$ bin/repl
> (require '[datomic.api :as d])

>  (def db-uri "datomic:sql://sample-tracking?jdbc:postgresql://<DB-IP>:5432/datomic?user=datomic&password=datomic")
>  (d/create-database db-uri)
```

## CI Deploy

The `.circleci` folder contains the `config.yaml` file that describes the deployment to the previously configured
cluster.

It requires a public IP for each environment `ereq-dev` and `ereq-prod`. Non `master` branches will be deployed to `dev`
for every commit, and `master` deploys to `prod`.

Each environment requires the environment variables in CircleCI to be configured appropriately. These are in the
CircleCI Contexts and Project Environment Variables.

The CI deploy uses [Google managed certificates](https://cloud.google.com/kubernetes-engine/docs/how-to/managed-certs)
and a [Google Ingress](https://cloud.google.com/kubernetes-engine/docs/how-to/load-balance-ingress) (as opposed to the
Nginx Ingress)

The HTTP-to-HTTPS redirect feature of the Ingress is still in beta and only available in GKE 1.18+. 1.18+ is still on
the Rapid release channel which can have some instability. To avoid that, we are using
a [manual partial LB](https://cloud.google.com/load-balancing/docs/https/setting-up-http-https-redirect#partial-http-lb)
. The summarized steps to setup this partial LB are:

* Ensure HTTP is not served on the Ingress using the annotiation `kubernetes.io/ingress.allow-http: "false"` on the
  Ingress
* Manually create a load balancer on the same IP as the Ingress with the HTTP-to-HTTPS redirect as described in the
  linked doc above.

## Non-CI Deploy

### Manually building Docker image

To build and package into Docker for dev:

```bash
$ npx webpack && lein package && docker build -t gcr.io/dev-project/sample-tracking:0.1.0 .
```

And for prod:

```bash
$ npx webpack && lein package && docker build -t gcr.io/production-project/sample-tracking:0.1.0 .
```

To push to GCR:

```bash
$ docker push <image-tag>
```

### Deploy the Peer Server and Datomic Services

```bash
$ kubectl apply -f ./deploy/k8s/datomic/transactor-service.yaml
$ kubectl apply -f ./deploy/k8s/datomic/peer.yaml
$ kubectl apply -f ./deploy/k8s/datomic/peer-service.yaml
```

### Run the Deploy Job

As of now this job transacts the schema to the database.

```bash
$ kubectl apply -f deploy/k8s/sample-tracking/deploy-job.yaml
```

To get the results of the job:

```bash
$kubectl get jobs

NAME           COMPLETIONS   DURATION   AGE
deploy-tasks   1/1           21s        55s
```

To get the pod name or check on the logs:

```bash
$ kubectl get pods
NAME                                 READY   STATUS    RESTARTS   AGE
datomic-peer-cb5cfc5b6-5shhm         1/1     Running   0          51m
datomic-transactor-c69857949-6cj6m   1/1     Running   0          71m
deploy-tasks-gjqg4                   1/1     Running   0          16s
```

```bash
$ kubectl logs deploy-tasks-gjqg4
[main] INFO org.eclipse.jetty.util.log - Logging initialized @5528ms to org.eclipse.jetty.util.log.Slf4jLog
20-03-04 00:53:00 deploy-tasks-gjqg4 INFO [org.parkerici.sample-tracking.cli:55] - Running with environment :default
20-03-04 00:53:00 deploy-tasks-gjqg4 INFO [org.parkerici.sample-tracking.db.schema:182] - Writing schema out to file.
20-03-04 00:53:00 deploy-tasks-gjqg4 INFO [org.parkerici.sample-tracking.db.schema:184] - Transacting schema.
```

Once it's successful, delete the job.

```bash
$ kubectl delete job deploy-tasks
```

### Deploy the App Server and Service

#### Deploying without a Domain Name

Deploy the app and the basic service to the cluster.

```bash
$ kubectl apply -f ./deploy/k8s/sample-tracking/app.yaml
$ kubectl apply -f ./deploy/k8s/sample-tracking/app-basic-service.yaml
```

Get the IP address for the service.

```bash
$ kubectl get service/sample-tracking

NAME              TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)        AGE
sample-tracking   LoadBalancer   10.110.5.220   34.82.204.132   80:31412/TCP   3m42s
```

#### Deploying with a Domain Name

Setup Helm locally.

```bash
$ brew install kubernetes-helm
```

Or make sure it's up to date if already installed.

```bash
$ brew upgrade kubernetes-helm
```

Reserve an **
unused/unbound** [reserved regional external IP from GCP](https://cloud.google.com/compute/docs/ip-addresses/reserve-static-external-ip-address)
IP address for the nginx load balancer.

```bash
gcloud compute addresses create sample-tracking --region <CLUSTER-REGION>
```

Install the nginx-ingress chart with the custom static IP. If you are installing multiple ingresses in the same culster
you must name them differently.

```bash
$ helm repo add stable https://kubernetes-charts.storage.googleapis.com
$ helm repo update
$ helm install nginx-ingress  stable/nginx-ingress --set controller.service.loadBalancerIP=<RESERVED-IP>
```

We can use the following command to check when our static IP has been assigned to the load balancer.

```bash
$ kubectl get services -o wide nginx-ingress-controller

NAME                          TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)                      AGE   SELECTOR
nginx-ingress-nginx-ingress   LoadBalancer   10.110.4.204   <RESERVED-IP>   80:31312/TCP,443:30326/TCP   85s   app=controller
```

Once this is done, create the application, service, and ingress to be exposed by the load balancer.

```bash
kubectl apply -f ./deploy/k8s/sample-tracking/app.yaml
kubectl apply -f ./deploy/k8s/sample-tracking/app-service.yaml
kubectl apply -f ./deploy/k8s/sample-tracking/app-ingress.yaml
```

## TODO

* Add test coverage
* Move all CircleCI environment variables into the Project Environment Variables.

# License

Mantis Viewer is distributed under Apache 2 license. See the [LICENSE](LICENSE.md) file for details.
