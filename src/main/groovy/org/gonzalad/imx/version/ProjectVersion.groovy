package com.up.imx.version

class ProjectVersion {
	
	final Integer major
	final Integer minor
	final String build

	ProjectVersion(Integer major, Integer minor, String build) {
		this.major = major
		this.minor = minor
		this.build = (build != null ? build : "0")
	}

	@Override
	String toString() {
		String fullVersion = "$major.$minor"
		if(build) {
			fullVersion += ".$build"
		}
		return fullVersion
	}
}
