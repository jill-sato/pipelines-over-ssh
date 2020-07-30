# Jenkins Pipelines Over SSH

## What does it solve?

If a Jenkins job uses SSH to execute a script remotely, and the hostname is hard-coded then
the job does not "scale".

I.e, this means there can at most one instance of such job running at any time, otherwise
 conflicts may occur on the remote machine:
 - ports
 - file systems
 - processes

Jenkins exposes machines (a.k.a "slaves" or "nodes") by connecting to a remote Java program that
 exposes a given machine to Jenkins.

It may be the case that SSH is used in build scripts to avoid installing Java on a system.

This plugin allows to dynamically allocate hostname to be used for the duration of a job or task.
 The allocated hostnames are available as environment variables so that scripts can easily be
 modified to "scale".

Effectively this provides a quick transition from "hard-coded SSH" towards pipelines: substitute
 hostnames with environment variables.

## Scaling Jenkins jobs

When a machine is managed by Jenkins, it is allocated to jobs based on a queue. Jobs can
 request machines using a name or tag. A job is queued until resources matching its criteria
 are available.

A job can "scale" when it requests machines using tag(s) (a.k.a "label"). Multiple instances of 
 such job can be running at the same time based on the resources available, queued job start when
 resources free-up. I.e. a job that "scales" runs concurrently given a demand and resources
  available.

Tags can be used to create pool of machines that have a common feature:
 - Operating system (E.g. `Linux`, `Windows`, `MacOS`, `Solaris`, `AIX`)
 - CPU architecture (`x86`, `x64`, `ppc`)
 - Distributions (`Ubuntu`, `Ubunt12`, `Centos`, `Centos7`, `Centos8`)

Tags can be used arbitrarily to groups machines to be used. A tag is basically a dynamic query
 mechanism to allocate machines.
 
## Multi-branch pipelines and the need for "scaling"

Multi-branch pipelines offer out-of-the-box integration with most SCM providers
 (`BitBucket`, `GitHub`, `GitLab`).

Pipelines can be triggered for pull requests and gate the merging of pull requests.
 E.g. You need one peer approval AND successful pipeline status to merge a pull request.

This model provides some basic coverage for the main branch and reduces the potential cascading
 issues. The more you run pre-merge, the fewer surprises you get later.

This model also simplifies job management for branches since the jobs lives with the code
 (I.e `Jenkinsfile`) and are detected automatically by Jenkins.

E.g. When creating a branch for a new version there is no need to create a new set of jobs.
 They are automtically detected when the new branch is pushed in the remote Git repository. 

## Implementation

This plugin provides a new type of "fake" node that exposes hostnames but executes the tasks on a
 "real" node (E.g. the `master` node).

This provides the ability to allocate machines that can be used "manually" with the SSH command, with a plain old
 Jenkins job (I.e not a multi-branch pipeline).

This plugin also provides a new task to execute code on a real node and allocate hostnames to be
 used "manually" with the SSH command with Jenkins pipelines.

E.g. Execute a script remotely on a node tagged with `ubuntu16` and `x64`: 
```groovy
sshNode([
    'ubuntu16_x64': ['ubuntu16', 'x64'],
]) {
    sshagent (credentials: ['ssh-credentials-id-xyz']) {
    sh '''
        scp run-tests.sh user@${ubuntu16_x64}:/tmp/ 
        ssh user@${ubuntu16_x64} /tmp/run-tests.sh
    '''
 }
}
```

E.g. Request 3 nodes at once for a multi-node setup
```groovy
sshNode([
    'ubuntu16_x64': ['ubuntu16', 'x64'],
    'ubuntu16_x32': ['ubuntu16', 'x32'],
    'centos7_x64': ['centos7', 'x64'],
]) {
    sshagent (credentials: ['ssh-credentials-id-xyz']) {
    sh '''
        ssh user@${ubuntu16_x64} "echo 'using Ubuntu16 x64 ..."
        ssh user@${ubuntu16_x32} "echo 'using Ubuntu16 x32 ..."
        ssh user@${centos7_x64} "echo 'using Centos7 x64 ..."
    '''
 }
}
```