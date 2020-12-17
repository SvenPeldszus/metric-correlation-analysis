package metric.correlation.analysis.tests.mocks;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class IProjectMock implements IProject {
	private IPath location;
	private String name;
	public IProjectMock(IPath location, String name) {
		this.location = location;
		this.name = name;
	}
	@Override
	public IResourceFilterDescription createFilter(int arg0, FileInfoMatcherDescription arg1, int arg2,
			IProgressMonitor arg3) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists(IPath arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IFile[] findDeletedMembersWithHistory(int arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResource findMember(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResource findMember(IPath arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResource findMember(String arg0, boolean arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResource findMember(IPath arg0, boolean arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDefaultCharset() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDefaultCharset(boolean arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFile getFile(IPath arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResourceFilterDescription[] getFilters() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFolder getFolder(IPath arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResource[] members() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResource[] members(boolean arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResource[] members(int arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setDefaultCharset(String arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDefaultCharset(String arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accept(IResourceVisitor arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accept(IResourceProxyVisitor arg0, int arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accept(IResourceProxyVisitor arg0, int arg1, int arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accept(IResourceVisitor arg0, int arg1, boolean arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accept(IResourceVisitor arg0, int arg1, int arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearHistory(IProgressMonitor arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copy(IPath arg0, boolean arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copy(IPath arg0, int arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copy(IProjectDescription arg0, boolean arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copy(IProjectDescription arg0, int arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IMarker createMarker(String arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResourceProxy createProxy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(boolean arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(int arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteMarkers(String arg0, boolean arg1, int arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IMarker findMarker(long arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMarker[] findMarkers(String arg0, boolean arg1, int arg2) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int findMaxProblemSeverity(String arg0, boolean arg1, int arg2) throws CoreException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getFileExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPath getFullPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getLocalTimeStamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IPath getLocation() {
		// TODO Auto-generated method stub
		return location;
	}

	@Override
	public URI getLocationURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMarker getMarker(long arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getModificationStamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public IContainer getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPathVariableManager getPathVariableManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPersistentProperty(QualifiedName arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IProject getProject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPath getProjectRelativePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPath getRawLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getRawLocationURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceAttributes getResourceAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getSessionProperty(QualifiedName arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IWorkspace getWorkspace() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAccessible() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDerived() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDerived(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isHidden() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isHidden(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLinked() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLinked(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLocal(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPhantom() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSynchronized(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTeamPrivateMember() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTeamPrivateMember(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isVirtual() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void move(IPath arg0, boolean arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(IPath arg0, int arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(IProjectDescription arg0, int arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(IProjectDescription arg0, boolean arg1, boolean arg2, IProgressMonitor arg3) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void refreshLocal(int arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void revertModificationStamp(long arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDerived(boolean arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDerived(boolean arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setHidden(boolean arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLocal(boolean arg0, int arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long setLocalTimeStamp(long arg0) throws CoreException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setPersistentProperty(QualifiedName arg0, String arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setReadOnly(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setResourceAttributes(ResourceAttributes arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSessionProperty(QualifiedName arg0, Object arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTeamPrivateMember(boolean arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void touch(IProgressMonitor arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T getAdapter(Class<T> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(ISchedulingRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isConflicting(ISchedulingRule arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void build(int arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void build(IBuildConfiguration arg0, int arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void build(int arg0, String arg1, Map<String, String> arg2, IProgressMonitor arg3) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearCachedDynamicReferences() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close(IProgressMonitor arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void create(IProgressMonitor arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void create(IProjectDescription arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void create(IProjectDescription arg0, int arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(boolean arg0, boolean arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IBuildConfiguration getActiveBuildConfig() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBuildConfiguration getBuildConfig(String arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBuildConfiguration[] getBuildConfigs() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IProjectDescription getDescription() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFile getFile(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFolder getFolder(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IProjectNature getNature(String arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBuildConfiguration[] getReferencedBuildConfigs(String arg0, boolean arg1) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IProject[] getReferencedProjects() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IProject[] getReferencingProjects() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPath getWorkingLocation(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasBuildConfig(String arg0) throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasNature(String arg0) throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isNatureEnabled(String arg0) throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void loadSnapshot(int arg0, URI arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(IProjectDescription arg0, boolean arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void open(IProgressMonitor arg0) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void open(int arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveSnapshot(int arg0, URI arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDescription(IProjectDescription arg0, IProgressMonitor arg1) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDescription(IProjectDescription arg0, int arg1, IProgressMonitor arg2) throws CoreException {
		// TODO Auto-generated method stub
		
	}

}
