package org.bbaw.wsp.cms.mdsystem.util;

/**
 * This record class contains all fields read by a config file.
 * 
 * @author Sascha Feldmann (wsp-shk1)
 * @date 26.01.2013
 * 
 */
public class MdsystemConfigRecord implements IMdsystemConfigRecord {
	private String eDocTemplatePath;
	private String modsStylesheetPath;
	private String metsStylesheetPath;
	private String datasetPath;
	private String larqIndexPath;
	private String normdataPath;

	public void setNormdataPath(String normdataPath) {
		this.normdataPath = normdataPath;
	}

	@Override
	public String geteDocTemplatePath() {
		return eDocTemplatePath;
	}

	public void seteDocTemplatePath(final String eDocTemplatePath) {
		this.eDocTemplatePath = eDocTemplatePath;
	}

	@Override
	public String getModsStylesheetPath() {
		return modsStylesheetPath;
	}

	public void setModsStylesheetPath(final String modsStylesheetPath) {
		this.modsStylesheetPath = modsStylesheetPath;
	}

	@Override
	public String getMetsStylesheetPath() {
		return metsStylesheetPath;
	}

	public void setMetsStylesheetPath(final String metsStylesheetPath) {
		this.metsStylesheetPath = metsStylesheetPath;
	}

	@Override
	public String getDatasetPath() {
		return datasetPath;
	}

	public void setDatasetPath(final String datasetPath) {
		this.datasetPath = datasetPath;
	}

	@Override
	public String getLarqIndexPath() {
		return larqIndexPath;
	}

	public void setLarqIndexPath(final String larqIndexPath) {
		this.larqIndexPath = larqIndexPath;
	}

	@Override
	public String toString() {
		return "MdsystemConfigRecord [eDocTemplatePath=" + eDocTemplatePath
				+ ", modsStylesheetPath=" + modsStylesheetPath
				+ ", metsStylesheetPath=" + metsStylesheetPath
				+ ", datasetPath=" + datasetPath + ", larqIndexPath="
				+ larqIndexPath + ", normdataPath=" + normdataPath + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((datasetPath == null) ? 0 : datasetPath.hashCode());
		result = prime
				* result
				+ ((eDocTemplatePath == null) ? 0 : eDocTemplatePath.hashCode());
		result = prime * result
				+ ((larqIndexPath == null) ? 0 : larqIndexPath.hashCode());
		result = prime
				* result
				+ ((metsStylesheetPath == null) ? 0 : metsStylesheetPath
						.hashCode());
		result = prime
				* result
				+ ((modsStylesheetPath == null) ? 0 : modsStylesheetPath
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final MdsystemConfigRecord other = (MdsystemConfigRecord) obj;
		if (datasetPath == null) {
			if (other.datasetPath != null) {
				return false;
			}
		} else if (!datasetPath.equals(other.datasetPath)) {
			return false;
		}
		if (eDocTemplatePath == null) {
			if (other.eDocTemplatePath != null) {
				return false;
			}
		} else if (!eDocTemplatePath.equals(other.eDocTemplatePath)) {
			return false;
		}
		if (larqIndexPath == null) {
			if (other.larqIndexPath != null) {
				return false;
			}
		} else if (!larqIndexPath.equals(other.larqIndexPath)) {
			return false;
		}
		if (metsStylesheetPath == null) {
			if (other.metsStylesheetPath != null) {
				return false;
			}
		} else if (!metsStylesheetPath.equals(other.metsStylesheetPath)) {
			return false;
		}
		if (modsStylesheetPath == null) {
			if (other.modsStylesheetPath != null) {
				return false;
			}
		} else if (!modsStylesheetPath.equals(other.modsStylesheetPath)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.bbaw.wsp.cms.mdsystem.util.IMdsystemConfigRecord#getNormdataPath()
	 */
	public String getNormdataPath() {
		return this.normdataPath;
	}

}
