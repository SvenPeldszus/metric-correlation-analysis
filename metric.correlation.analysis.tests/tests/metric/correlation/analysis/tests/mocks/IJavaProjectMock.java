package metric.correlation.analysis.tests.mocks;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.eval.IEvaluationContext;

public class IJavaProjectMock implements IJavaProject {

	private IProject project;

	public IJavaProjectMock(IProject project) {
		this.project = project;
	}

	public IJavaProjectMock() {
	}

	@Override
	public IJavaElement[] getChildren() throws JavaModelException {
		return new IJavaElement[0];
	}

	@Override
	public boolean hasChildren() throws JavaModelException {
		return false;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public IJavaElement getAncestor(int arg0) {
		return null;
	}

	@Override
	public String getAttachedJavadoc(IProgressMonitor arg0) throws JavaModelException {
		return null;
	}

	@Override
	public IResource getCorrespondingResource() throws JavaModelException {
		return null;
	}

	@Override
	public String getElementName() {
		return null;
	}

	@Override
	public int getElementType() {
		return 0;
	}

	@Override
	public String getHandleIdentifier() {
		return null;
	}

	@Override
	public IJavaModel getJavaModel() {
		return null;
	}

	@Override
	public IJavaProject getJavaProject() {
		return this;
	}

	@Override
	public IOpenable getOpenable() {
		return null;
	}

	@Override
	public IJavaElement getParent() {
		return null;
	}

	@Override
	public IPath getPath() {
		return null;
	}

	@Override
	public IJavaElement getPrimaryElement() {
		return null;
	}

	@Override
	public IResource getResource() {
		return null;
	}

	@Override
	public ISchedulingRule getSchedulingRule() {
		return null;
	}

	@Override
	public IResource getUnderlyingResource() throws JavaModelException {
		return null;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public boolean isStructureKnown() throws JavaModelException {
		return false;
	}

	@Override
	public <T> T getAdapter(Class<T> arg0) {
		return null;
	}

	@Override
	public void close() throws JavaModelException {
	}

	@Override
	public String findRecommendedLineSeparator() throws JavaModelException {
		return null;
	}

	@Override
	public IBuffer getBuffer() throws JavaModelException {
		return null;
	}

	@Override
	public boolean hasUnsavedChanges() throws JavaModelException {
		return false;
	}

	@Override
	public boolean isConsistent() throws JavaModelException {
		return false;
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public void makeConsistent(IProgressMonitor arg0) throws JavaModelException {
	}

	@Override
	public void open(IProgressMonitor arg0) throws JavaModelException {
	}

	@Override
	public void save(IProgressMonitor arg0, boolean arg1) throws JavaModelException {
	}

	@Override
	public IClasspathEntry decodeClasspathEntry(String arg0) {
		return null;
	}

	@Override
	public Set<String> determineModulesOfProjectsWithNonEmptyClasspath() throws JavaModelException {
		return Collections.emptySet();
	}

	@Override
	public String encodeClasspathEntry(IClasspathEntry arg0) {
		return null;
	}

	@Override
	public IJavaElement findElement(IPath arg0) throws JavaModelException {
		return null;
	}

	@Override
	public IJavaElement findElement(IPath arg0, WorkingCopyOwner arg1) throws JavaModelException {
		return null;
	}

	@Override
	public IJavaElement findElement(String arg0, WorkingCopyOwner arg1) throws JavaModelException {
		return null;
	}

	@Override
	public IModuleDescription findModule(String arg0, WorkingCopyOwner arg1) throws JavaModelException {
		return null;
	}

	@Override
	public IPackageFragment findPackageFragment(IPath arg0) throws JavaModelException {
		return null;
	}

	@Override
	public IPackageFragmentRoot findPackageFragmentRoot(IPath arg0) throws JavaModelException {
		return null;
	}

	@Override
	public IPackageFragmentRoot[] findPackageFragmentRoots(IClasspathEntry arg0) {
		return new IPackageFragmentRoot[0];
	}

	@Override
	public IType findType(String arg0) throws JavaModelException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IType findType(String arg0, IProgressMonitor arg1) throws JavaModelException {
		return null;
	}

	@Override
	public IType findType(String arg0, WorkingCopyOwner arg1) throws JavaModelException {
		return null;
	}

	@Override
	public IType findType(String arg0, String arg1) throws JavaModelException {
		return null;
	}

	@Override
	public IType findType(String arg0, WorkingCopyOwner arg1, IProgressMonitor arg2) throws JavaModelException {
		return null;
	}

	@Override
	public IType findType(String arg0, String arg1, IProgressMonitor arg2) throws JavaModelException {
		return null;
	}

	@Override
	public IType findType(String arg0, String arg1, WorkingCopyOwner arg2) throws JavaModelException {
		return null;
	}

	@Override
	public IType findType(String arg0, String arg1, WorkingCopyOwner arg2, IProgressMonitor arg3)
			throws JavaModelException {
		return null;
	}

	@Override
	public IPackageFragmentRoot[] findUnfilteredPackageFragmentRoots(IClasspathEntry arg0) {
		return new IPackageFragmentRoot[0];
	}

	@Override
	public IPackageFragmentRoot[] getAllPackageFragmentRoots() throws JavaModelException {
		return new IPackageFragmentRoot[0];
	}

	@Override
	public IClasspathEntry getClasspathEntryFor(IPath arg0) throws JavaModelException {
		return null;
	}

	@Override
	public IModuleDescription getModuleDescription() throws JavaModelException {
		return null;
	}

	@Override
	public Object[] getNonJavaResources() throws JavaModelException {
		return new Object[0];
	}

	@Override
	public String getOption(String arg0, boolean arg1) {
		return null;
	}

	@Override
	public Map<String, String> getOptions(boolean arg0) {
		return Collections.emptyMap();
	}

	@Override
	public IPath getOutputLocation() throws JavaModelException {
		return null;
	}

	@Override
	public IModuleDescription getOwnModuleDescription() throws JavaModelException {
		return null;
	}

	@Override
	public IPackageFragmentRoot getPackageFragmentRoot(String arg0) {
		return null;
	}

	@Override
	public IPackageFragmentRoot getPackageFragmentRoot(IResource arg0) { // TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPackageFragmentRoot[] getPackageFragmentRoots() throws JavaModelException {
		return new IPackageFragmentRoot[0];
	}

	@Override
	public IPackageFragmentRoot[] getPackageFragmentRoots(IClasspathEntry arg0) {
		return new IPackageFragmentRoot[0];
	}

	@Override
	public IPackageFragment[] getPackageFragments() throws JavaModelException {
		return new IPackageFragment[0];
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public IClasspathEntry[] getRawClasspath() throws JavaModelException {
		return new IClasspathEntry[0];
	}

	@Override
	public IClasspathEntry[] getReferencedClasspathEntries() throws JavaModelException {
		return new IClasspathEntry[0];
	}

	@Override
	public String[] getRequiredProjectNames() throws JavaModelException {
		return new String[0];
	}

	@Override
	public IClasspathEntry[] getResolvedClasspath(boolean arg0) throws JavaModelException {
		return new IClasspathEntry[0];
	}

	@Override
	public boolean hasBuildState() {
		return false;
	}

	@Override
	public boolean hasClasspathCycle(IClasspathEntry[] arg0) {
		return false;
	}

	@Override
	public boolean isOnClasspath(IJavaElement arg0) {
		return false;
	}

	@Override
	public boolean isOnClasspath(IResource arg0) {
		return false;
	}

	@Override
	public IEvaluationContext newEvaluationContext() {
		return null;
	}

	@Override
	public ITypeHierarchy newTypeHierarchy(IRegion arg0, IProgressMonitor arg1) throws JavaModelException {
		return null;
	}

	@Override
	public ITypeHierarchy newTypeHierarchy(IRegion arg0, WorkingCopyOwner arg1, IProgressMonitor arg2)
			throws JavaModelException {
		return null;
	}

	@Override
	public ITypeHierarchy newTypeHierarchy(IType arg0, IRegion arg1, IProgressMonitor arg2) throws JavaModelException {
		return null;
	}

	@Override
	public ITypeHierarchy newTypeHierarchy(IType arg0, IRegion arg1, WorkingCopyOwner arg2, IProgressMonitor arg3)
			throws JavaModelException {
		return null;
	}

	@Override
	public IPath readOutputLocation() {
		return null;
	}

	@Override
	public IClasspathEntry[] readRawClasspath() {
		return new IClasspathEntry[0];
	}

	@Override
	public void setOption(String arg0, String arg1) {
	}

	@Override
	public void setOptions(Map<String, String> arg0) {
	}

	@Override
	public void setOutputLocation(IPath arg0, IProgressMonitor arg1) throws JavaModelException {
	}

	@Override
	public void setRawClasspath(IClasspathEntry[] arg0, IProgressMonitor arg1) throws JavaModelException {
	}

	@Override
	public void setRawClasspath(IClasspathEntry[] arg0, boolean arg1, IProgressMonitor arg2) throws JavaModelException {
	}

	@Override
	public void setRawClasspath(IClasspathEntry[] arg0, IPath arg1, IProgressMonitor arg2) throws JavaModelException {
	}

	@Override
	public void setRawClasspath(IClasspathEntry[] arg0, IPath arg1, boolean arg2, IProgressMonitor arg3)
			throws JavaModelException {
	}

	@Override
	public void setRawClasspath(IClasspathEntry[] arg0, IClasspathEntry[] arg1, IPath arg2, IProgressMonitor arg3)
			throws JavaModelException {
	}

}