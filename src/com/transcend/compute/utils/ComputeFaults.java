package com.transcend.compute.utils;

import com.msi.tough.query.ErrorResponse;
import com.msi.tough.query.QueryFaults;

public class ComputeFaults extends QueryFaults{

	public static ErrorResponse VolumeDoesNotExist(String volId) {
		return new ErrorResponse("Sender",
				"The specified volume  " + volId + " does not exist.",
				"InvalidVolume.NotFound");
	}

	public static class InstanceDoesNotExist extends ErrorResponse {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        /**
         * @param vmId
         */
        public InstanceDoesNotExist(String vmId) {
            super("Sender",
                    "The specified instance " + vmId + " does not exist.",
                    "InvalidInstance.NotFound");
        }
	}

	public static ErrorResponse instanceDoesNotExist(String vmId) {
		return new InstanceDoesNotExist(vmId);
	}

	public static ErrorResponse GroupAlreadyExists(String groupId) {
		return new ErrorResponse("Sender",
				"The group " + groupId + " already exists",
				"InvalidGroup.Duplicate");
	}

	public static Exception GroupIdDoesNotExist(String groupId) {
		return new ErrorResponse("Sender",
				"The group with the ID " + groupId + " does not exist",
				"InvalidGroup.NotFound");
	}

	public static Exception GroupNameDoesNotExist(String groupName) {
		return new ErrorResponse("Sender",
				"The group by the name of " + groupName + " does not exist",
				"InvalidGroup.NotFound");
	}

	public static Exception GroupInUse() {
		return new ErrorResponse("Sender",
				"The group specified is not available for deletion",
				"InvalidGroup.InUse");
	}

	public static Exception VolumeCannotBeDeleted(String volId) {
		return new ErrorResponse("Sender",
				"The volume with the ID " + volId + " cannot be deleted.",
				"IncorrectState");
	}

	public static Exception IpAddressDoesNotExist(String address) {
		return new ErrorResponse("Sender",
				"The IP Address " + address + " is not an available address",
				"InvalidIPAddress.NotFound");
	}

	public static Exception RuleAlreadyExists(String msg) {
		return new ErrorResponse("Sender", msg, "RuleAlreadyExists");
	}

	public static Exception RuleDoesNotExists(String msg) {
		return new ErrorResponse("Sender", msg, "RuleDoesNotExist");
	}

}
