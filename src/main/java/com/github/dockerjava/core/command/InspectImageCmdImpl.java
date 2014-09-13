package com.github.dockerjava.core.command;

import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;

import com.google.common.base.Preconditions;

/**
 * Inspect the details of an image.
 */
public class InspectImageCmdImpl extends AbstrDockerCmd<InspectImageCmd, InspectImageResponse> implements InspectImageCmd  {

	private String imageId;

	public InspectImageCmdImpl(InspectImageCmd.Exec exec, String imageId) {
		super(exec);
		withImageId(imageId);
	}

    @Override
	public String getImageId() {
        return imageId;
    }

    @Override
	public InspectImageCmd withImageId(String imageId) {
		Preconditions.checkNotNull(imageId, "imageId was not specified");
		this.imageId = imageId;
		return this;
	}

    @Override
    public String toString() {
        return "inspect " + imageId;
    }
    
    /**
     * @throws NotFoundException No such image
     */
	@Override
    public InspectImageResponse exec() throws NotFoundException {
    	return super.exec();
    }
}