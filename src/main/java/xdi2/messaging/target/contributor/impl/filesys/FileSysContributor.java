package xdi2.messaging.target.contributor.impl.filesys;

import java.io.File;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.Graph;
import xdi2.core.features.nodetypes.XdiAbstractEntity;
import xdi2.core.features.nodetypes.XdiEntity;
import xdi2.core.features.nodetypes.XdiEntityCollection;
import xdi2.core.features.nodetypes.XdiEntityInstance;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.util.GraphUtil;
import xdi2.messaging.operations.GetOperation;
import xdi2.messaging.target.MessagingTarget;
import xdi2.messaging.target.Prototype;
import xdi2.messaging.target.contributor.ContributorMount;
import xdi2.messaging.target.contributor.ContributorResult;
import xdi2.messaging.target.contributor.impl.AbstractContributor;
import xdi2.messaging.target.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.execution.ExecutionContext;
import xdi2.messaging.target.impl.graph.GraphMessagingTarget;

@ContributorMount(contributorXDIAddresses={"(#test)"})
public class FileSysContributor extends AbstractContributor implements Prototype<FileSysContributor> {

	private static final Logger log = LoggerFactory.getLogger(FileSysContributor.class);

	private static final String DEFAULT_BASE_PATH = ".";
	private static final String DEFAULT_GRAPH_PATH = null;

	private static final XDIAddress XDI_ADD_EC_DIR = XDIAddress.create("[#dir]");
	private static final XDIAddress XDI_ADD_EC_FILE = XDIAddress.create("[#file]");
	private static final XDIAddress XDI_ADD_AS_NAME = XDIAddress.create("<#name>");
	private static final XDIAddress XDI_ADD_AS_SIZE = XDIAddress.create("<#size>");

	private String basePath;
	private String graphPath;

	public FileSysContributor() {

		super();

		this.basePath = DEFAULT_BASE_PATH;
		this.graphPath = DEFAULT_GRAPH_PATH;

		this.getContributors().addContributor(new FileSysDirContributor());
	}

	/*
	 * Prototype
	 */

	@Override
	public FileSysContributor instanceFor(PrototypingContext prototypingContext) throws Xdi2MessagingException {

		// create new contributor

		FileSysContributor contributor = new FileSysContributor();

		// set the something

		//	contributor.setTokenGraph(this.getTokenGraph());

		// done

		return contributor;
	}

	/*
	 * Init and shutdown
	 */

	@Override
	public void init(MessagingTarget messagingTarget) throws Exception {

		super.init(messagingTarget);

		// determine base path

		if (this.getBasePath() == null) throw new Xdi2MessagingException("No base path.", null, null);
		if (! this.getBasePath().endsWith("/")) this.setBasePath(this.getBasePath() + "/");

		// determine graph path

		if (this.getGraphPath() == null && messagingTarget instanceof GraphMessagingTarget) {

			String relativeGraphPath = URLEncoder.encode(GraphUtil.getOwnerXDIAddress(((GraphMessagingTarget) messagingTarget).getGraph()).toString(), "UTF-8");
			this.setGraphPath(this.getBasePath() + relativeGraphPath);
		}

		if (this.getGraphPath() == null) throw new Xdi2MessagingException("No graph path.", null, null);
	}

	/*
	 * Sub-Contributors
	 */

	@ContributorMount(contributorXDIAddresses={"#dir"})
	private class FileSysDirContributor extends AbstractContributor {

		private FileSysDirContributor() {

			super();

			this.getContributors().addContributor(new FileSysOtherContributor());
		}

		@Override
		public ContributorResult executeGetOnAddress(XDIAddress[] contributorAddresses, XDIAddress contributorsAddress, XDIAddress relativeTargetAddress, GetOperation operation, Graph operationResultGraph, ExecutionContext executionContext) throws Xdi2MessagingException {

			XDIAddress fileSysContextXDIAddress = contributorAddresses[contributorAddresses.length - 2];
			XDIAddress fileSysDirContextXDIAddress = contributorAddresses[contributorAddresses.length - 1];

			log.debug("fileSysContextXDIAddress: " + fileSysContextXDIAddress + ", fileSysDirContextXDIAddress: " + fileSysDirContextXDIAddress);

			// map the directory

			File graphRootDir = new File(FileSysContributor.this.getGraphPath());

			XdiEntity xdiEntity = XdiAbstractEntity.fromContextNode(operationResultGraph.setDeepContextNode(contributorsAddress));

			mapDir(graphRootDir, xdiEntity);

			// done

			return ContributorResult.SKIP_MESSAGING_TARGET;
		}
	}

	@ContributorMount(contributorXDIAddresses={"{}"})
	private class FileSysOtherContributor extends AbstractContributor {

		private FileSysOtherContributor() {

			super();
		}

		@Override
		public ContributorResult executeGetOnAddress(XDIAddress[] contributorAddresses, XDIAddress contributorsAddress, XDIAddress relativeTargetAddress, GetOperation operation, Graph operationResultGraph, ExecutionContext executionContext) throws Xdi2MessagingException {

			XDIAddress fileSysContextXDIAddress = contributorAddresses[contributorAddresses.length - 2];
			XDIAddress fileSysDirContextXDIAddress = contributorAddresses[contributorAddresses.length - 1];

			log.debug("fileSysContextXDIAddress: " + fileSysContextXDIAddress + ", fileSysDirContextXDIAddress: " + fileSysDirContextXDIAddress);

			// parse identifiers

			// done

			return new ContributorResult(true, false, true);
		}
	}

	/*
	 * Helper methods
	 */

	private static void mapDir(File dir, XdiEntity xdiEntity) {

		XdiEntityCollection dirXdiEntityCollection = xdiEntity.getXdiEntityCollection(XDI_ADD_EC_DIR, true);
		XdiEntityCollection fileXdiEntityCollection = xdiEntity.getXdiEntityCollection(XDI_ADD_EC_FILE, true);

		for (File file : dir.listFiles()) {

			if (file.isDirectory()) {

				if (log.isDebugEnabled()) log.debug("In " + dir.getAbsolutePath() + ": Directory: " + file.getAbsolutePath());

				XdiEntityInstance dirXdiEntityMember = dirXdiEntityCollection.setXdiInstanceUnordered(true, false, XDIArc.literalFromRandomUuid());
				dirXdiEntityMember.getXdiAttribute(XDI_ADD_AS_NAME, true).setLiteralString(file.getName());
				dirXdiEntityMember.getXdiAttribute(XDI_ADD_AS_SIZE, true).setLiteralNumber(Double.valueOf(file.getTotalSpace()));

				mapDir(file, dirXdiEntityMember);
			}

			if (file.isFile()) {

				if (log.isDebugEnabled()) log.debug("In " + dir.getAbsolutePath() + ": File: " + file.getAbsolutePath());

				XdiEntityInstance fileXdiEntityMember = fileXdiEntityCollection.setXdiInstanceUnordered(true, false, XDIArc.literalFromRandomUuid());
				fileXdiEntityMember.getXdiAttribute(XDI_ADD_AS_NAME, true).setLiteralString(file.getName());
				fileXdiEntityMember.getXdiAttribute(XDI_ADD_AS_SIZE, true).setLiteralNumber(Double.valueOf(file.getTotalSpace()));
			}
		}
	}

	/*
	 * Getters and setters
	 */

	public String getBasePath() {

		return this.basePath;
	}

	public void setBasePath(String basePath) {

		this.basePath = basePath;
	}

	public String getGraphPath() {

		return this.graphPath;
	}

	public void setGraphPath(String graphPath) {

		this.graphPath = graphPath;
	}
}
