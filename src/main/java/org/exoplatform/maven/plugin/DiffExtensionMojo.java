package org.exoplatform.maven.plugin;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.repository.RepositoryManager;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mojo( name = "check")
public class DiffExtensionMojo extends AbstractMojo {

    /**
     * Set to true if the build should abort on diff found.
     */
    @Parameter(defaultValue="true")
    private boolean abortBuildOnDiff;

    /**
     * Source Version : the version from which we are doing the upgrade
     */
    @Parameter(required = true)
    private String sourceVersion;

    /**
     * Target Version : the version to which we are doing the upgrade
     */
    @Parameter(required = true)
    private String targetVersion;

    /**
     * Target folder
     *
     * @since 2.0-alpha-5
     */
    @Parameter( defaultValue = "${project.build.directory}/diffExtension", readonly = true )
    private File outputDirectory;

    /**
     * The Maven session
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    /**
     * Remote repositories which will be searched for artifacts.
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true )
    private List<ArtifactRepository> remoteRepositories;

    @Component
    private RepositoryManager repositoryManager;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;


    @Component
    private ArtifactResolver artifactResolver;
    /**
     * To look up Archiver/UnArchiver implementations
     */
    @Component
    private ArchiverManager archiverManager;


    /**
     * Files to check
     */
    @Parameter(defaultValue="")
    private List<FileToCheck> files;



    public void execute() throws MojoExecutionException,MojoFailureException {
        if (files==null) {
            files=new ArrayList<>();
        }

        getLog().info( "Checking upgrade from " + sourceVersion+" to "+targetVersion);
        List<FileToCheck> changedFiles = new ArrayList<>();
        for (FileToCheck f : files) {
            Artifact artifactOldVersion= getArtifact(f.groupId,f.artifactId,sourceVersion,f.type);
            Artifact artifactNewVersion= getArtifact(f.groupId,f.artifactId,targetVersion,f.type);
            File oldVersion = unpack(artifactOldVersion);
            File newVersion = unpack(artifactNewVersion);

            String oldFilePath = oldVersion.getPath()+f.path;
            String newFilePath = newVersion.getPath()+f.path;

            File fileOldVersion = new File(oldFilePath);
            File fileNewVersion = new File(newFilePath);

            String oldMd5=fileMD5Sum(fileOldVersion);
            String newMd5=fileMD5Sum(fileNewVersion);
            if (oldMd5 == null ||newMd5 == null) {
                getLog().warn("Unable to get md5Sum for "+f.path);
                f.setPatch(extractPatch(fileOldVersion,fileNewVersion));
                changedFiles.add(f);
            } else if (!oldMd5.equals(newMd5)) {
                f.setPatch(extractPatch(fileOldVersion,fileNewVersion));
                changedFiles.add(f);
            } else {
                getLog().info("File " + f.groupId+":"+f.artifactId+f.path + " doesn't change between " + sourceVersion + " " +
                        "and " + targetVersion);
            }
        }

        if (changedFiles.size()>0) {
            for (FileToCheck fileToCheck : changedFiles) {
                getLog().warn("File " + fileToCheck.groupId + ":" + fileToCheck.artifactId + fileToCheck.path + " changes between " + sourceVersion + " and " + targetVersion + ". Need to " +
                        "check.");
                if (fileToCheck.getPatch()!=null){
                    for (Delta delta : fileToCheck.getPatch().getDeltas()) {
                        getLog().warn(String.format("diff: \n\t[oldVersion] -> %s\n\t[newVersion] -> %s",
                                delta.getOriginal().toString(),
                                delta.getRevised().toString()));
                    }
                }
            }
            if (abortBuildOnDiff) {
                throw new MojoFailureException("Diff exists. Please check");
            }
        }

    }

    private Patch extractPatch(File fileOldVersion, File fileNewVersion) {
        try {
            List<String> oldLines = toLines(fileOldVersion);
            List<String> newLines = toLines(fileNewVersion);
            return DiffUtils.diff(oldLines, newLines);
        } catch (Exception e) {
            getLog().error("Unable to read files "+fileOldVersion + " and "+fileNewVersion,e);
        }
        return null;

    }

    private List<String> toLines(File file) throws Exception {
        List<String> lines = new ArrayList<String>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {}
            }
        }
        return lines;
    }

    public File unpack(Artifact artifact) throws MojoExecutionException {
        File file=artifact.getFile();
        String path =
                outputDirectory.getPath()+"/"+artifact.getGroupId()+"-"+artifact.getArtifactId()+"-"+artifact.getVersion();
        File artifactFile = new File(path);
        getLog().debug("Unpack artifact "+artifact.getGroupId()+":"+artifact.getArtifactId()+":"+artifact.getVersion());
        try {

            if (artifactFile.exists()) {
                getLog().debug(artifactFile.getPath()+" exists, delete it");
                FileUtils.deleteDirectory(artifactFile);
            }
            artifactFile.mkdirs();
            if (!artifactFile.exists()) {
                throw new MojoExecutionException( "Location to write unpacked files to could not be created: "
                        + artifactFile );
            }

            UnArchiver unArchiver;

            try {
                unArchiver = archiverManager.getUnArchiver(artifact.getType());
                getLog().debug("Found unArchiver by type: " + unArchiver);
            }
            catch (NoSuchArchiverException e) {
                unArchiver = archiverManager.getUnArchiver(file);
                getLog().debug("Found unArchiver by extension: " + unArchiver);
            }
            unArchiver.setSourceFile(file);
            unArchiver.setDestDirectory(artifactFile);
            unArchiver.extract();
            return artifactFile;
        }
        catch (NoSuchArchiverException e){
            throw new MojoExecutionException("Unknown archiver type", e);
        }
        catch (ArchiverException e) {
            throw new MojoExecutionException( "Error unpacking file: " + file + " to: " + artifactFile
                    + System.lineSeparator() + e.toString(), e );
        } catch (IOException e) {
            e.printStackTrace();
            throw new MojoExecutionException("Unable to delete existing folder "+artifactFile.getPath(), e);

        }
    }


    protected Artifact getArtifact(String groupId, String artifactId, String version, String type)
            throws MojoExecutionException {
        Artifact artifact;

        try
        {

            ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();

            // Map dependency to artifact coordinate
            DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
            coordinate.setGroupId(groupId);
            coordinate.setArtifactId(artifactId);
            coordinate.setVersion(version);

            final String extension;
            ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler(type);
            if (artifactHandler != null)
            {
                extension = artifactHandler.getExtension();
            }
            else
            {
                extension = type;
            }
            coordinate.setExtension(extension);

            artifact = artifactResolver.resolveArtifact( buildingRequest, coordinate ).getArtifact();
        }
        catch (ArtifactResolverException e)
        {
            throw new MojoExecutionException( "Unable to find/resolve artifact.", e);
        }

        return artifact;
    }

    /**
     * @return Returns a new ProjectBuildingRequest populated from the current session and the current project remote
     *         repositories, used to resolve artifacts.
     */
    public ProjectBuildingRequest newResolveArtifactProjectBuildingRequest()
    {
        ProjectBuildingRequest buildingRequest =
                new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );
        buildingRequest.setRemoteRepositories(remoteRepositories);
        return buildingRequest;
    }

    public String fileMD5Sum(File file) {
        try {
            InputStream is = new FileInputStream(file);
            String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(is);
            return md5;
        } catch (Exception e) {
            getLog().error("Unable to get md5sum for "+file.getPath());
        }
        return null;
    }
}

