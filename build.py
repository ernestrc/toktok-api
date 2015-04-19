#!/usr/bin/env python2.7
import os
import subprocess
import shutil
import sys

PROJECTS = {'gateway': 'io.toktok.gateway.Boot',
            'analytics': 'io.toktok.analytics.Boot',
            'notifications': 'io.toktok.notifications.Boot',
            'users_command': 'io.toktok.command.users.Boot',
            'users_query': 'io.toktok.query.users.Boot'}

TEMPLATE = 'krakken/deploy/microservice/Dockerfile'

SCALA = '2.11'

REPO = 'toktok'


def parseVersion():
    with open('version.sbt', 'r') as versionSbt:
        v = versionSbt.read().split(':=')[-1].strip().strip('\"')
        print 'Version is {}'.format(v)
        return v


def buildDockerfile(pr, v):
    pro_dockerfile = './target/Dockerfile-' + pr
    shutil.copy(TEMPLATE, pro_dockerfile)
    with open(pro_dockerfile) as f:
        a = f.read().replace(
            '@SERVICE', pr).replace(
            '@SCALA', SCALA).replace(
            '@MAINCLASS', PROJECTS[pr]).replace(
            '@VERSION', v).replace(
            '@PWD', os.getcwd())

    with open(pro_dockerfile, 'w') as f:
        f.write(a)

    print 'Copied {} to {}'.format(TEMPLATE, pro_dockerfile)
    return pro_dockerfile


def buildImage(df, pr, v):
    tag = '{}/{}:{}'.format(REPO, pr, v)
    cmd = 'docker build --file={} -t {} .'.format(df, tag)
    p = subprocess.Popen(cmd, shell=True)
    return p.wait()


def pushImage(pr, v):
    cmd = 'docker push {}/{}:{}'.format(REPO, pr, v)
    p = subprocess.Popen(cmd, shell=True)
    return p.wait()


def checkStatus(status, msg):
    if status != 0:
        print msg
        sys.exit(2)


def main():
    p = subprocess.Popen('sbt assembly', shell=True)
    status = p.wait()
    status = 0
    print 'Finished building jars. Status code is %s' % (status)

    version = parseVersion()

    for project in PROJECTS.keys():

        dockerfile = buildDockerfile(project, version)

        build_status = buildImage(dockerfile, project, version)

        checkStatus(build_status,
                    '\nThere was problem when building image for {}'.format(
                        project))

        push_status = pushImage(project, version)

        checkStatus(push_status,
                    '\nThere was a problem when pushing image!')


if __name__ == '__main__':
    main()
