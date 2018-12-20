def jenkinsfile


def overrides = [
    scriptVersion  : 'AOS-3004',
    pipelineScript: 'https://git.aurora.skead.no/scm/ao/aurora-pipeline-scripts.git',
    credentialsId: "github",
    sonarQube: false,
    debugAndExit:true,
    versionStrategy: [
      [ branch: 'master', versionHint: '1' ]
    ]
]

fileLoader.withGit(overrides.pipelineScript,, overrides.scriptVersion) {
   jenkinsfile = fileLoader.load('templates/leveransepakke')
}
jenkinsfile.gradle(overrides.scriptVersion, overrides)
