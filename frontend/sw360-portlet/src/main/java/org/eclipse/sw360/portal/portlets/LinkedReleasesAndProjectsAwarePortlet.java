/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLink;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectLink;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyString;
import static org.eclipse.sw360.portal.common.PortalConstants.PARENT_BRANCH_ID;
import static org.eclipse.sw360.portal.common.PortalConstants.PROJECT_LIST;
import static org.eclipse.sw360.portal.common.PortalConstants.RELEASE_LIST;

/**
 * linked releases and projects-aware portlet implementation
 *
 * @author alex.borodin@evosoft.com
 */
public abstract class LinkedReleasesAndProjectsAwarePortlet extends AttachmentAwarePortlet {

    private static final Logger log = Logger.getLogger(LinkedReleasesAndProjectsAwarePortlet.class);

    protected LinkedReleasesAndProjectsAwarePortlet() {
        this(new ThriftClients());
    }

    public LinkedReleasesAndProjectsAwarePortlet(ThriftClients thriftClients) {
        super(thriftClients);
    }


    protected boolean isLinkedObjectsAwareAction(String action) {
        return action.startsWith(PortalConstants.LINKED_OBJECTS_PREFIX);
    }

    @Override
    protected boolean isGenericAction(String action) {
        return super.isGenericAction(action) || isLinkedObjectsAwareAction(action);
    }

    @Override
    protected void dealWithGenericAction(ResourceRequest request, ResourceResponse response, String action) throws IOException, PortletException {
        if (super.isGenericAction(action)) {
            super.dealWithGenericAction(request, response, action);
        } else {
            dealWithLinkedObjects(request, response, action);
        }
    }

    protected void dealWithLinkedObjects(ResourceRequest request, ResourceResponse response, String action) throws PortletException, IOException {
        if (PortalConstants.LOAD_LINKED_PROJECTS_ROWS.equals(action)) {
            serveLoadLinkedProjectsRows(request, response);
        } else if (PortalConstants.LOAD_LINKED_RELEASES_ROWS.equals(action)) {
            serveLoadLinkedReleasesRows(request, response);
        }
    }

    protected void putDirectlyLinkedReleaseRelationsInRequest(PortletRequest request, Release release) {
        List<ReleaseLink> linkedReleaseRelations = SW360Utils.getLinkedReleaseRelations(release, thriftClients, log);
        request.setAttribute(RELEASE_LIST, linkedReleaseRelations);
    }

    protected void putDirectlyLinkedReleasesInRequest(PortletRequest request, Project project) throws TException {
        List<ReleaseLink> linkedReleases = SW360Utils.getLinkedReleases(project, thriftClients, log);
        request.setAttribute(RELEASE_LIST, linkedReleases);
    }

    protected List<ProjectLink> createLinkedProjects(Project project, User user) {
        return createLinkedProjects(project, Function.identity(), user);
    }

    protected List<ProjectLink> createLinkedProjects(Project project, Function<ProjectLink, ProjectLink> projectLinkMapper, User user) {
        return createLinkedProjects(project, projectLinkMapper, false, user);
    }

    protected List<ProjectLink> createLinkedProjects(Project project, Function<ProjectLink, ProjectLink> projectLinkMapper, boolean deep,
            User user) {
        final Collection<ProjectLink> linkedProjects = SW360Utils.getLinkedProjectsAsFlatList(project, deep, thriftClients, log, user);
        return linkedProjects.stream().map(projectLinkMapper).collect(Collectors.toList());
    }

    protected void putDirectlyLinkedProjectsInRequest(PortletRequest request, Project project, User user) {
        final Collection<ProjectLink> linkedProjects = SW360Utils.getLinkedProjects(project, false, thriftClients, log, user);
        List<ProjectLink> secondLevelLinks = linkedProjects
                .stream()
                .map(ProjectLink::getSubprojects)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        request.setAttribute(PROJECT_LIST, secondLevelLinks);
    }

    public Function<ProjectLink, ProjectLink> createProjectLinkMapper(Function<ReleaseLink, ReleaseLink> releaseLinkMapper){
        return (projectLink) -> {
            List<ReleaseLink> mappedReleaseLinks = nullToEmptyList(projectLink
                    .getLinkedReleases())
                    .stream()
                    .map(releaseLinkMapper)
                    .collect(Collectors.toList());
            projectLink.setLinkedReleases(mappedReleaseLinks);
            return projectLink;
        };
    }

    protected void serveLoadLinkedProjectsRows(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        prepareLinkedProjects(request);
        include("/html/utils/ajax/linkedProjectsRows.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }

    protected void prepareLinkedProjects(ResourceRequest request) throws PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        String branchId = request.getParameter(PARENT_BRANCH_ID);
        Optional<String> projectIdOpt = getProjectIdFromBranchId(branchId);
        request.setAttribute(PARENT_BRANCH_ID, branchId);
        final Project project;
        if (projectIdOpt.isPresent()) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                project = client.getProjectById(projectIdOpt.get(), user);
            } catch (TException e) {
                log.error("Error getting projects!", e);
                throw new PortletException("cannot load project " + projectIdOpt.get(), e);
            }
        } else {
            project = new Project();
        }

        List<ProjectLink> mappedProjectLinks = createLinkedProjects(project, user);
        request.setAttribute(PROJECT_LIST, mappedProjectLinks);
        request.setAttribute(PortalConstants.PARENT_SCOPE_GROUP_ID, request.getParameter(PortalConstants.PARENT_SCOPE_GROUP_ID));
    }

    protected Optional<String> getProjectIdFromBranchId(String branchId) {
        String[] split = nullToEmptyString(branchId).split("_");
        return split.length > 0 ? Optional.of(split[0]) : Optional.empty();
    }

    protected void serveLoadLinkedReleasesRows(ResourceRequest request, ResourceResponse response) throws PortletException, IOException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        String branchId = request.getParameter(PARENT_BRANCH_ID);
        request.setAttribute(PARENT_BRANCH_ID, branchId);
        if (branchId != null) {
            String id = branchId.split("_")[0];
            try {
                ComponentService.Iface client = thriftClients.makeComponentClient();
                Release release = client.getReleaseById(id, user);
                putDirectlyLinkedReleaseRelationsInRequest(request, release);
            } catch (TException e) {
                log.error("Error getting projects!", e);
                throw new PortletException("cannot get projects", e);
            }
        } else {
            putDirectlyLinkedReleaseRelationsInRequest(request, new Release());
        }

        request.setAttribute(PortalConstants.PARENT_SCOPE_GROUP_ID, request.getParameter(PortalConstants.PARENT_SCOPE_GROUP_ID));
        include("/html/utils/ajax/linkedReleasesRows.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }
}
