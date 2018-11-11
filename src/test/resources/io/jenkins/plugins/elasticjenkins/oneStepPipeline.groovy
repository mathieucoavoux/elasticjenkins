/*
pipeline {
    agent any
    stages {
        stage('Stage1') {
            steps {
                wrap([$class: 'ElasticJenkinsWrapper', 'stepName' : 'create_ec2_instance']) {

                        echo "Hello"


                }
            }
        }
    }
}
*/

node {
    stage('Stage1') {
        wrap([$class: 'ElasticJenkinsWrapper', 'stepName' : 'create_ec2_instance']) {
            echo "Hello"
        }
    }
}

