package com.commsen.maven.plugin.bomhelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goal which resolves dependencies declared in dependency management (BOM)
 */

@Mojo(
		name = "resolve",
		requiresProject = true,
		defaultPhase = LifecyclePhase.VALIDATE)
public class BomResolveMojo extends BomHelperAbstaractMojo {

	private static final Logger logger = LoggerFactory.getLogger(BomResolveMojo.class);
	
	/**
	 * Remote repositories which will be searched for artifacts.
	 */
	@Parameter(
			defaultValue = "${project.remoteArtifactRepositories}",
			readonly = true,
			required = true)
	protected List<ArtifactRepository> remoteRepositories;


	@Component
	private ArtifactResolver artifactResolver;

	
	public void execute() throws MojoExecutionException {
		List<Dependency> bomDepenedencies =  project.getDependencyManagement().getDependencies();
		Set<String> failedArtifacts = new HashSet<>();
		ProjectBuildingRequest projectBuildingRequest = newResolveArtifactProjectBuildingRequest();
		
		for (Dependency dependency : bomDepenedencies) {
			DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
			coordinate.setGroupId(dependency.getGroupId());
			coordinate.setArtifactId(dependency.getArtifactId());
			coordinate.setVersion(dependency.getVersion());
			coordinate.setExtension(dependency.getType());
			coordinate.setClassifier(dependency.getClassifier());
			try {
				artifactResolver.resolveArtifact(projectBuildingRequest, coordinate);
			} catch (ArtifactResolverException e) {
				failedArtifacts.add(dependency.toString());
				logger.error("Failed to resolve artifact " + coordinate, e);
			}
		}

		if (!failedArtifacts.isEmpty()) {
			throw new MojoExecutionException(
					"The following dependencies found in <dependencyManagement> can not be resolved: \n - " +
					failedArtifacts.stream().collect(Collectors.joining("\n - "))
					);
		}
		
	}


	private ProjectBuildingRequest newResolveArtifactProjectBuildingRequest() {
		ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

		buildingRequest.setRemoteRepositories(remoteRepositories);

		return buildingRequest;
	}
}
