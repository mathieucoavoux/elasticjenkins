node('!master') {
    wrap([$class: 'ElasticJenkinsWrapper', 'stepName' : 'create_ec2_instance']) {
        echo "Hello"
    }
    //always {
        wrap([$class: 'ElasticJenkinsWrapper']) {
            echo "Operation completed"
        }
    //}
}
