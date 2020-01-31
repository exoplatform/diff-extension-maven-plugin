Readme
=======

This project is a maven plugin, which is designed to help developers to track modifications on an overriden file between 2
 versions.
 
This plugin will notify the developer during his project's build, if a change occurs on a tracked file.
If a change is displayed, you know that you have to adapt your override to get the last version of the file.



Utilisation
===========
Add this in the build part of your project : 
```
    <plugin>
        <groupId>org.exoplatform.maven.plugin</groupId>
        <artifactId>diff-extension-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <executions>
          <execution>
            <phase>process-resources</phase>
            <goals>
              <goal>check</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          ...
        </configuration>
      </plugin>
```

Configuration
=============
The configuration part contains few parameters :
```
    <configuration>
          <abortBuildOnDiff>false</abortBuildOnDiff>
          <sourceVersion>5.2.1</sourceVersion>
          <targetVersion>${org.exoplatform.platform.version}</targetVersion>
          <files>
            <fileToCheck>
              <groupId>org.exoplatform.platform</groupId>
              <artifactId>platform-extension-portlets-platformNavigation</artifactId>
              <path>/groovy/platformNavigation/portlet/UISpaceNavigationPortlet/UISpaceNavigationPortlet.gtmpl</path>
              <type>war</type>
            </fileToCheck>
            <fileToCheck>
              <groupId>org.exoplatform.platform</groupId>
              <artifactId>platform-extension-portlets-platformNavigation</artifactId>
              <path>/groovy/platformNavigation/portlet/UIUserNavigationPortlet/UIUserNavigationPortlet.gtmpl</path>
              <type>war</type>
            </fileToCheck>
          </files>
        </configuration>
```

Configuration properties : 

Property | Description | Default Value
-------- | ----------- | -------------
abortBuildOnDiff | Should the build fails when a diff is founded | true
sourceVersion  | The version before the upgrade | Required, no default value
targetVersion  | The version after the upgrade | Required, no default value
files  | The files to check. List here all overriden templates or configuration files | 

FileToCheck properties :

Property | Description 
-------- | ----------- 
groupId | The groupId 
artifactId | The artifactId 
type | The type of the artifact
path | the path of the overriden file in the artifact

Execution
=============
 When you compile your project, the plugin will check, for each listed file, if the original file change between the old
  version and the new version (by using a md5sum). If there is a difference, it will display the list of modifications, and
   break the build if abortBuildOnDiff is set to true.
   
Output Example :
=============
The configuration in example make this output :
```
[INFO] --- diff-extension-maven-plugin:1.0-SNAPSHOT:check (default) @ functional-configuration-webapp ---
[INFO] Checking upgrade from 5.2.1 to 6.0.0-M12
[WARNING] File org.exoplatform.platform:platform-extension-portlets-platformNavigation/groovy/platformNavigation/portlet/UISpaceNavigationPortlet/UISpaceNavigationPortlet.gtmpl changes between 5.2.1 and 6.0.0-M12. Need to check.
[WARNING] diff: 
        [oldVersion] -> [position: 77, size: 1, lines: [                                        <a href="javascript:void(0);" onclick="$link" class="spaceIcon avatarMini" ><img data-container="body" alt="" src="$spaceImageSource" data-placement="right" rel="tooltip" data-toggle="tooltip" title="$spaceDisplayName"/><span data-placement="bottom" data-container="body" rel="tooltip" data-toggle="tooltip" data-original-title="$spaceDisplayName"> $spaceDisplayName</span></a>]]
        [newVersion] -> [position: 77, size: 1, lines: [                                        <a href="$spaceUrl" class="spaceIcon avatarMini" ><img data-container="body" alt="" src="$spaceImageSource" data-placement="right" rel="tooltip" data-toggle="tooltip" title="$spaceDisplayName"/><span data-placement="bottom" data-container="body" rel="tooltip" data-toggle="tooltip" data-original-title="$spaceDisplayName"> $spaceDisplayName</span></a>]]
[WARNING] diff: 
        [oldVersion] -> [position: 68, size: 1, lines: [                String link = uicomponent.event("SelectSpace", space.getId());]]
        [newVersion] -> [position: 68, size: 1, lines: [                 def spaceUrl = uicomponent.buildSpaceURL(space);]]
[WARNING] File org.exoplatform.platform:platform-extension-portlets-platformNavigation/groovy/platformNavigation/portlet/UIUserNavigationPortlet/UIUserNavigationPortlet.gtmpl changes between 5.2.1 and 6.0.0-M12. Need to check.
[WARNING] diff: 
        [oldVersion] -> [position: 117, size: 1, lines: [    <% if  ((nodeName.equals(uicomponent.DASHBOARD_URI) || nodeName.equals(uicomponent.NOTIFICATION_SETTINGS))&&(!isOwner))  continue;]]
        [newVersion] -> [position: 117, size: 1, lines: [    <% if  ((nodeName.equals(uicomponent.WALLET_URI) || nodeName.equals(uicomponent.GAMIFICATION_URI) ||   nodeName.equals(uicomponent.NOTIFICATION_SETTINGS)) && (!isOwner))  continue;]]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------

```

