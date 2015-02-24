include_defs('//bucklets/gerrit_plugin.bucklet')

gerrit_plugin(
  name = 'reviewassistant',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/**/*']),
  manifest_entries = [
    'Implementation-Title: Reviewer Assistant',
    'Implementation-URL: https://github.com/reviewassistant/reviewassistant',
    'Gerrit-PluginName: reviewassistant',
    'Gerrit-Module: com.github.reviewassistant.reviewassistant.Module',
    'Gerrit-HttpModule: com.github.reviewassistant.reviewassistant.HttpModule',
  ],
)

java_library(
  name = 'classpath',
  deps = [':reviewassistant__plugin'],
)

