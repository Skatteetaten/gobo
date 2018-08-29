def jenkinsfile


def overrides = [
    scriptVersion  : 'feature/AOS-2708',
    pipelineScript: 'https://git.aurora.skead.no/scm/ao/aurora-pipeline-scripts.git',
    openShiftBaseImage: 'yeaster',
    openShiftBaseVersion: '1',
    disableAllReports: true,
    credentialsId: "github",
    suggestVersionAndTagReleases: [
      [branch: 'master', versionHint: '1.0']
    ]
]

fileLoader.withGit(overrides.pipelineScript,, overrides.scriptVersion) {
   jenkinsfile = fileLoader.load('templates/leveransepakke')
}
jenkinsfile.gradle(overrides.scriptVersion, overrides)
