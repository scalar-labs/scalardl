# Scalar DL Network Operational Guide

Here you will find our operational guide to running a Scalar DL Network. This guide will cover the network overview, planning, configuring, deploying, operating, and maintaining a Scalar DL environment. By the end you should be ready to plan, build, and operate your own Scalar DL Network.  

### Table Of Contents
* [Scalar DL Network Overview](#Scalar-DL-Network-Overview)
* [Cluster Resource Planning](#Cluster-Resource-Planning)
* [Cluster Configuration](#Cluster-Configuration)
* [Initial Cluster Deployment](#Initial-Cluster-Deployment)
* [Monitoring and Alerting](#Monitoring-And-Alerting)
* [Stateless Resource Maintenance](#Stateless-Resource-Maintenance)
* [Stateful Resource Maintenance](#Stateful-Resource-Maintenance)
* [Day-To-Day Operations](#Day-To-Day-Operations)

# Scalar DL Network Overview
The Scalar DL Network is made up of many different resources such as Cassandra, Scalar DL, API gateway, and monitoring.

A typical deployment will create a three node Cassandra cluster, a three node Scalar DL cluster, a Traefik entry point, and a bastion node. It is also common to deploy other services like Prometheus (monitoring), an alertmanager (alerting), and Grafana (data visualization).

It is possible to selectively choose which of these services you want to deploy, but at minimum you need one *Cassandra* and *Scalar DL* resource to get started.

Resources are separated into two categories, stateful and stateless. A stateful resource, is a resource that contains irreplaceable generated data, such as, user content. In the case of the Scalar DL Network, the stateful resource is the `cassandra` database cluster. A stateless resource, is a resource that operates without the need of any persistent storage, such as the Scalar DL cluster.  

![](./resources/scalar_cluster.png)

# Resource Planning
Before building your Scalar DL environment, you need to plan what type of resources you need. We provide 3 different resource levels depending on the expected server load.

### Entry Level (Recommended)
-description-

* point 1
* point 2
### Medium Level
-description-

* point 1
* point 2
### High Level
-description-

* point 1
* point 2


# Cluster Configuration
To help get started, we provide Terraform scripts to build the environment. This means all you need to do is make a configuration file and simply deploy with a few commands. Below we describe the configuration file used in Scalar DL deployment.

## Requirements
In order to use Scalar DL deployment scripts, you will first need to install the necessary tools. Please follow the installation guide provided by each tool.  

1) [Terraform](https://www.terraform.io/downloads.html) (v0.11.11) - Used to provision cloud resources
2) [Packer](https://www.packer.io/downloads.html) (v1.3.4) - Create cloud specific resource images
3) [Ansible](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html) (v2.7) - Used to provision VM Image

## Directory Setup
In many cases, you will want to deploy multiple environments, e.g., one for testing, one for staging, and one for production. To keep things organized and sharable between team members, we recommend the following folder structure.

```
+ root_project
  + test (environment_name)
    - config.json
  + staging
    - config.json
  + production
    - config.json
```
## Config File
The config file is the main file you need to modify to deploy your environment. The file is used to specify the type and quantity of resources you wish to deploy. Please refer to the [Cluster Resource Planning Section](#Cluster-Resource-Planning) as needed.

Example Config
```json
{
  "name": "Deployment Name",
  "prefix": "short-name",
  "environment": "sandbox",
  "tag": "<docker-tag>",
  "aws_key_name": "<key_name>",
  "scalar_instance_type": "t2.large",
  "scalar_ebs_volume_size": "50",
  "scalar_autoscaling_group_desire": "3",
  "cassandra_instance_type": "t3.xlarge",
  "cassandra_ebs_volume_size": "1000",
  "cassandra_instance_count": "3",
  "docker_user": "docker_hub_username",
  "docker_password": "docker_hub_password",
  "private_key_file": "~/.ssh/private.pem"
}
```

### Required Values

#### name
This can be a descriptive name for your deployment.

#### prefix
The prefix is a short name used to attach to all resources. It shouldn't contain spaces or special characters.  

#### environment
A simple one word to categories deployments.

#### tag
The tag is used to specify the docker tag to deploy.

#### aws_key_name
This must match a key_name that is managed by your AWS account.

#### docker_user
The docker_hub user name to pull docker images

#### docker_password
The docker_hub password to pull docker images

#### private_key_file
The path to the private key file used for SSH

### Optional Values

#### cassandra_instance_type
The resource type to deploy, e.g., t2.large

#### cassandra_ebs_volume_size
The size of the ebs volume for Cassandra in GB

#### cassandra_instance_count
The number of Cassandra resources to start

#### scalar_autoscaling_group_desire
The desired number of resources for scalar DL service

#### scalar_instance_type
Resource Type of scalar DL service: t2.micro

#### scalar_ebs_volume_size
The size of scalar ebs volume in GB

#### site_url
A DNS entry that defines this deployment. Only used if you want a public facing application.

#### aws_readonly_key
The IAM user key that gives Prometheus rights to monitor the cloud resources

#### aws_readonly_secret
The IAM user secret that gives Prometheus rights to monitor the cloud resources  

#### grafana_password
The initial admin password used to configure Grafana

#### slack_webhook_url
The slack webhook URL

# Initial Scalar DL Network Deployment
The Scalar DL Network deployment is taken care of by Terraform and the configuration file you created in the previous section. Currently we support AWS, and in the future we will support other cloud services. The deployment of the Scalar DL Network is a collection of Terraform modules, both local and remote, that define your infrastructure. In order for Terraform to build your environment, it must pull all remote modules. This is taken care of when you run `terraform init`.

```bash
# This will setup terraform and pull any remote modules
terraform init
```
* You need to run `terraform init` whenever a remote module changes

Basically, Terraform runs in two steps: planning and applying. First you make a terraform plan and then apply the plan to your environment.
```bash
terraform plan --var-file config.json -out plan.out
```
* Terraform will prompt you for any missing required config keys.

```bash
terraform apply plan.out
```  
* At this point, it will take a few minutes to apply the plan but by the end all your cloud resources should be up and running.  


### Terraform Plan
When Terraform makes a plan, it will check the currently running resources against a `state` file and base the new plan to match the desired changes.

* For example, if you were to shutdown a resource the next time you run `terraform plan`, it will detect that the resource is not in the correct state and make a plan to `start` the resource.

The state file is very important for terraform and should be kept in a remote backend, [Terraform S3 Backend](https://www.terraform.io/docs/backends/types/s3.html). It is generally not a good idea to check this state file into a git repo. The state file is Terraform's only knowledge of your infrastructure and without it, Terraform wouldn't be able to determine which resources it is managing. Adding the state file to a git repo is prone to errors, as every branch or fork may have a slightly different version. Using a remote backend resolves this problem, as there is only ever one copy of the state file. You may also be interested in [state file locking](https://www.terraform.io/docs/backends/state.html), which goes one step further, by preventing two team members from changing the same environment at the same time.  

##### Example Backend Config (optional but recommended)

 * Create `backend.tf` in the project environment root folder, in this case `staging`, and copy the below code.

```code
terraform {
  backend "s3" {
    bucket = "s3-bucket-name"
    key = "myproject/staging/terraform.tfstate"
    region = "s3-bucket-region"
  }
}
```
* Don't forget to run `terraform init` after you create this file.
* It is possible to change the backend but be careful not to overwrite an existing backend. By default Terraform will overwrite the destination, which may remove an existing state file.
* `backend.tf` is unable to read Terraform's user variables, so it is not possible to set these values in the `config.json`.

# Monitoring And Alerting
In order to provide a production ready environment, we provide options to deploy with monitoring and alerting. We use Prometheus to collect metrics from all the deployed resources. By using Prometheus, we can take advantage of the vast collection of community developed exporters. An exporter, is an agent that is loaded onto a host resource, that exposes metrics via a REST api endpoint. An exporter, can be built into your application or run separately, making Prometheus a versatile tool.

## Exporters
We use three types of exporters for the Scalar DL Network.

* `node_exporter` collects host metrics, CPU, memory, disk, network, and running services.
* `cassandra_exporter` that can monitor cassandra using JMX.
* `traefik_exporter` that can monitor the API gateway service.

## Alertmanager
The alertmanager, is a service that works alongside Prometheus. Its sole purpose is to send triggered alerts from prometheus to some external provider, such as Slack or Email. We provide basic alerting scripts that monitor for down services, low drive space, and high CPU usage. You will also be able to add custom alerting in the future.

## Grafana
Grafana, is a tool to help visualize Prometheus's metrics using the graphing framework. Grafana links directly into Prometheus, allowing you to query all collected metrics. The benefit of using Prometheus with Grafana, is that most `exporters` come with a grafana dashboard, making it easy to setup.  

# Stateless Resource Maintenance
In general, the stateless resources are easier to maintain because nothing is stored on the resource, making it safer and faster to replace. In a Scalar DL Network, the only stateful resource is `cassandra` database cluster. Please see [Stateful Resource Maintenance](#Stateful-Resource-Maintenance). We will cover typical maintenance tasks, such as, what to do if a resource goes down.

## Scalar DL Resources
If you deploy with Prometheus and configure the alertmanager you will be notified if a Scalar DL resource is down. The Scalar service is automatically restarted, but in some cases the underlying host may be in a bad state that will require manual action.

#### Example
Let's say `scalar server 1` is being alerted as down. There are several options you can take to get the resource up and running. The first is just to run the `terraform plan` again, to see if a plan can be generated to fix the issue. The other options are a more forceful way to tell Terraform to replace a resource. These include `taint` or manually termination.

* See if terraform can recover (recommended first action)
```bash
# This may not work if the resource is in a running state but the services is still unable to run.

terraform plan --var-file config.json -out plan.out
terraform apply plan.out
```
* Use `terraform taint`, flag resource in state file to be recreated
```bash
terraform taint -module=scalar aws_instance.scalar.0 # Server number minus 1
terraform plan --var-file config.json -out plan.out
terraform apply plan.out
```
* Manual termination followed by terraform plan
```bash
> Use AWS console to manual terminate resource
> Prometheus alert should show aws instance-id
terraform plan --var-file config.json -out plan.out
terraform apply plan.out
```

##### Taint Commands
Here are common taint commands that will flag a resource in the state file to be recreated the next time you make a plan. There is also an `untaint` command in case you want to remove the flag.  

```bash
terraform taint -module=scalar aws_instance.scalar.{resource number - 1}
terraform taint -module=service.prometheus aws_instance.instance
terraform taint -module=service.traefik aws_instance.instance
terraform taint -module=service.grafana aws_instance.instance

terraform untaint -module=scalar aws_instance.scalar.0
```

# Stateful Resource Maintenance
Coming Soon

# Day To Day Operations
Coming Soon


# ***TODO NOTES***

### Sho-chiku-bai

* Recommended cluster


### Cluster Procedures

* How to setup a DL Cluster
* How to maintain/operate Cluster
* How to recover from failures
* Day by Day operations
