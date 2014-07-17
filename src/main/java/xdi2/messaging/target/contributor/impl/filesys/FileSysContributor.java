package xdi2.messaging.target.contributor.impl.filesys;

import java.io.File;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.features.nodetypes.XdiAbstractEntity;
import xdi2.core.features.nodetypes.XdiAbstractMemberUnordered;
import xdi2.core.features.nodetypes.XdiEntity;
import xdi2.core.features.nodetypes.XdiEntityCollection;
import xdi2.core.features.nodetypes.XdiEntityMember;
import xdi2.core.util.GraphUtil;
import xdi2.core.xri3.XDI3Segment;
import xdi2.messaging.GetOperation;
import xdi2.messaging.MessageResult;
import xdi2.messaging.context.ExecutionContext;
import xdi2.messaging.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.MessagingTarget;
import xdi2.messaging.target.Prototype;
import xdi2.messaging.target.contributor.AbstractContributor;
import xdi2.messaging.target.contributor.ContributorMount;
import xdi2.messaging.target.contributor.ContributorResult;
import xdi2.messaging.target.impl.graph.GraphMessagingTarget;

@ContributorMount(contributorXris={"(#test)"})
public class FileSysContributor extends AbstractContributor implements Prototype<FileSysContributor> {

	private static final Logger log = LoggerFactory.getLogger(FileSysContributor.class);

	private static final String DEFAULT_BASE_PATH = ".";
	private static final String DEFAULT_GRAPH_PATH = null;

	private static final XDI3Segment XRI_S_EC_DIR = XDI3Segment.create("[#dir]");
	private static final XDI3Segment XRI_S_EC_FILE = XDI3Segment.create("[#file]");
	private static final XDI3Segment XRI_S_AS_NAME = XDI3Segment.create("<#name>");
	private static final XDI3Segment XRI_S_AS_SIZE = XDI3Segment.create("<#size>");

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

			String relativeGraphPath = URLEncoder.encode(GraphUtil.getOwnerXri(((GraphMessagingTarget) messagingTarget).getGraph()).toString(), "UTF-8");
			this.setGraphPath(this.getBasePath() + relativeGraphPath);
		}

		if (this.getGraphPath() == null) throw new Xdi2MessagingException("No graph path.", null, null);
	}

	/*
	 * Sub-Contributors
	 */

	@ContributorMount(contributorXris={"#dir"})
	private class FileSysDirContributor extends AbstractContributor {

		private FileSysDirContributor() {

			super();

			this.getContributors().addContributor(new FileSysOtherContributor());
		}

		@Override
		public ContributorResult executeGetOnAddress(XDI3Segment[] contributorXris, XDI3Segment contributorsXri, XDI3Segment relativeTargetAddress, GetOperation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

			XDI3Segment fileSysContextXri = contributorXris[contributorXris.length - 2];
			XDI3Segment fileSysDirContextXri = contributorXris[contributorXris.length - 1];

			log.debug("fileSysContextXri: " + fileSysContextXri + ", fileSysDirContextXri: " + fileSysDirContextXri);

			// map the directory

			File graphRootDir = new File(FileSysContributor.this.getGraphPath());

			XdiEntity xdiEntity = XdiAbstractEntity.fromContextNode(messageResult.getGraph().setDeepContextNode(contributorsXri));

			mapDir(graphRootDir, xdiEntity);

			// done

			return ContributorResult.SKIP_MESSAGING_TARGET;
		}
	}

	@ContributorMount(contributorXris={"{}"})
	private class FileSysOtherContributor extends AbstractContributor {

		private FileSysOtherContributor() {

			super();
		}

		@Override
		public ContributorResult executeGetOnAddress(XDI3Segment[] contributorXris, XDI3Segment contributorsXri, XDI3Segment relativeTargetAddress, GetOperation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

			XDI3Segment fileSysContextXri = contributorXris[contributorXris.length - 2];
			XDI3Segment fileSysDirContextXri = contributorXris[contributorXris.length - 1];

			log.debug("fileSysContextXri: " + fileSysContextXri + ", fileSysDirContextXri: " + fileSysDirContextXri);

			// parse identifiers

			// done

			return new ContributorResult(true, false, true);
		}
	}

	/*
	 * Helper methods
	 */

	private static void mapDir(File dir, XdiEntity xdiEntity) {

		XdiEntityCollection dirXdiEntityCollection = xdiEntity.getXdiEntityCollection(XRI_S_EC_DIR, true);
		XdiEntityCollection fileXdiEntityCollection = xdiEntity.getXdiEntityCollection(XRI_S_EC_FILE, true);

		for (File file : dir.listFiles()) {

			if (file.isDirectory()) {

				XdiEntityMember dirXdiEntityMember = dirXdiEntityCollection.setXdiMemberUnordered(XdiAbstractMemberUnordered.createRandomUuidArcXri(false));
				dirXdiEntityMember.getXdiAttribute(XRI_S_AS_NAME, true).getXdiValue(true).setLiteralString(file.getName());
				dirXdiEntityMember.getXdiAttribute(XRI_S_AS_SIZE, true).getXdiValue(true).setLiteralNumber(Double.valueOf(file.getTotalSpace()));

				mapDir(file, dirXdiEntityMember);
			}

			if (file.isFile()) {

				XdiEntityMember fileXdiEntityMember = fileXdiEntityCollection.setXdiMemberUnordered(XdiAbstractMemberUnordered.createRandomUuidArcXri(false));
				fileXdiEntityMember.getXdiAttribute(XRI_S_AS_NAME, true).getXdiValue(true).setLiteralString(file.getName());
				fileXdiEntityMember.getXdiAttribute(XRI_S_AS_SIZE, true).getXdiValue(true).setLiteralNumber(Double.valueOf(file.getTotalSpace()));
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
