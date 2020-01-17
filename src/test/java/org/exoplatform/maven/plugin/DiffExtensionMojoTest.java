package org.exoplatform.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DiffExtensionMojoTest {

    private DiffExtensionMojo diffExtensionMojo;

    @Before
    public void setUp() {

        diffExtensionMojo = new DiffExtensionMojo();
    }


    @Test
    public void TestSameFile() throws  MojoExecutionException, NoSuchFieldException {
        try {
            List<FileToCheck> files = new ArrayList<>();
            FileToCheck file = new FileToCheck();
            file.groupId = "testGroupId";
            file.artifactId = "testArtifactId";
            file.path = "";
            file.type = "testType";
            files.add(file);
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("files"), files);

            String sourceVersion = "testSourceVersion";
            String targetVersion = "testTargetVersion";
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("sourceVersion"), sourceVersion);
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("targetVersion"), targetVersion);

            boolean abortBuildOnDiff = true;
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("abortBuildOnDiff"), abortBuildOnDiff);


            DiffExtensionMojo spyedDiffExtensionMojo = Mockito.spy(diffExtensionMojo);
            doReturn(null).when(spyedDiffExtensionMojo).getArtifact(any(), any(), any(), any());

            doReturn(new File("./src/test/resources/file1.gtmpl"), new File("./src" +
                    "/test/resources/file1.gtmpl")).when(spyedDiffExtensionMojo).unpack(any());
            spyedDiffExtensionMojo.execute();

        } catch (MojoFailureException e) {
            Assert.fail("Files are different, but should be identical");
        }
    }

    @Test
    public void TestDifferentFileWithAbortBuild() throws  MojoExecutionException, NoSuchFieldException {
        try {
            List<FileToCheck> files = new ArrayList<>();
            FileToCheck file = new FileToCheck();
            file.groupId = "testGroupId";
            file.artifactId = "testArtifactId";
            file.path = "";
            file.type = "testType";
            files.add(file);
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("files"), files);

            String sourceVersion = "testSourceVersion";
            String targetVersion = "testTargetVersion";
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("sourceVersion"), sourceVersion);
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("targetVersion"), targetVersion);

            boolean abortBuildOnDiff = true;
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("abortBuildOnDiff"), abortBuildOnDiff);

            DiffExtensionMojo spyedDiffExtensionMojo = Mockito.spy(diffExtensionMojo);
            doReturn(null).when(spyedDiffExtensionMojo).getArtifact(any(), any(), any(), any());

            doReturn(new File("./src/test/resources/file1.gtmpl"), new File("./src" +
                    "/test/resources/file2.gtmpl")).when(spyedDiffExtensionMojo).unpack(any());
            spyedDiffExtensionMojo.execute();
            Assert.fail("Files are identical, but should be different");

        } catch (MojoFailureException e) {
            //we should be here
        }
    }
    @Test
    public void TestDifferentFileWithoutAbortBuild() throws  MojoExecutionException, NoSuchFieldException {
        try {
            List<FileToCheck> files = new ArrayList<>();
            FileToCheck file = new FileToCheck();
            file.groupId = "testGroupId";
            file.artifactId = "testArtifactId";
            file.path = "";
            file.type = "testType";
            files.add(file);
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("files"), files);

            String sourceVersion = "testSourceVersion";
            String targetVersion = "testTargetVersion";
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("sourceVersion"), sourceVersion);
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("targetVersion"), targetVersion);

            boolean abortBuildOnDiff = false;
            FieldSetter.setField(diffExtensionMojo, diffExtensionMojo.getClass().getDeclaredField("abortBuildOnDiff"), abortBuildOnDiff);


            DiffExtensionMojo spyedDiffExtensionMojo = Mockito.spy(diffExtensionMojo);
            doReturn(null).when(spyedDiffExtensionMojo).getArtifact(any(), any(), any(), any());

            doReturn(new File("./src/test/resources/file1.gtmpl"), new File("./src" +
                    "/test/resources/file2.gtmpl")).when(spyedDiffExtensionMojo).unpack(any());
            spyedDiffExtensionMojo.execute();

        } catch (MojoFailureException e) {
            Assert.fail("Files are different, but we shouldn't abort on build");
        }
    }
}
