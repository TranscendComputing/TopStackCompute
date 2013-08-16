package com.transcend.compute.utils;

import java.util.List;

import org.dasein.cloud.network.Firewall;
import org.dasein.cloud.network.FirewallSupport;

import com.generationjava.io.xml.XMLNode;
import com.msi.tough.query.QueryUtil;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsResponseMessage.SecurityGroupInfo;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsResponseMessage.SecurityGroupInfo.IpPermission;
import com.transcend.compute.message.DescribeSecurityGroupsMessage.DescribeSecurityGroupsResponseMessage.SecurityGroupInfo.IpRange;

public class SecurityGroupUtils {

	public static void marshallSecurityGroups(final XMLNode root,
			final List<SecurityGroupInfo> grps) {
		final XMLNode info = QueryUtil.addNode(root, "securityGroupInfo");
		for (final SecurityGroupInfo g : grps) {
			final XMLNode i = QueryUtil.addNode(info, "item");
			QueryUtil.addNode(i, "description", g.getGroupDescription());
			QueryUtil.addNode(i, "groupId", g.getGroupId());
			QueryUtil.addNode(i, "groupName", g.getGroupName());
			QueryUtil.addNode(i, "ownerId", g.getOwnerId());
			final XMLNode ips = QueryUtil.addNode(i, "ipPermissions");
			for (final IpPermission p : g.getIpPermissionList()) {
				final XMLNode pn = QueryUtil.addNode(ips, "item");
				QueryUtil.addNode(pn, "fromPort", p.getFromPort());
				QueryUtil.addNode(pn, "ipProtocol", p.getIpProtocol());
				QueryUtil.addNode(pn, "toPort", p.getToPort());
				final XMLNode rs = QueryUtil.addNode(pn, "ipRanges");
				for (final IpRange r : p.getIpRangeList()) {
					final XMLNode rn = QueryUtil.addNode(rs, "item");
					QueryUtil.addNode(rn, "cidrIp", r.getCidrIp());
				}
			}
		}
	}
	
	/*public static List<IpPermission> parseIpPermissions(final Map<String, String[]> mapIn){
		final List<IpPermission> ipPermissions = new ArrayList<IpPermission>();
		final String s = "IpPermissions.";

		for(int n = 1;;n++){
			final IpPermission.Builder permission = IpPermission.newBuilder();
			//if all 3 of these keys don't exist, assume there are no more IpPermissions to be sorted through
			if(!mapIn.containsKey(s + n + ".IpProtocol") && !mapIn.containsKey(s + n + ".FromPort") && !mapIn.containsKey(s + n + ".ToPort")){
				break;
			}
			permission.setIpProtocol(QueryUtil.getString(mapIn, s + n + ".IpProtocol"));
			permission.setToPort(QueryUtil.getInt(mapIn, s + n + ".ToPort", -2));
			permission.setFromPort(QueryUtil.getInt(mapIn, s + n + ".FromPort", -2));

			final List<String> cidrList = new ArrayList<String>();
			final List<Group> groups = new ArrayList<Group>();
			for(int m = 1;;m++){
				final String g = s + n + ".Groups." + m + ".";
				//if map doesn't have IpPermissions.n.Groups.m or IpPermissions.n.IpRanges.m, break the loop, assume there's no more Groups or IpRanges
				//TODO: This check could probably be cleaned up.  
				if(!mapIn.containsKey(g + "UserId") && !mapIn.containsKey(g + "GroupId") && !mapIn.containsKey(g + "GroupName")
						&& !mapIn.containsKey(s + n + ".IpRanges." + m + ".CidrIp")){
					break;
				}
				final Group.Builder group = Group.newBuilder();
				group.setUserId(QueryUtil.getString(mapIn,  g + "UserId"));
				group.setGroupName(QueryUtil.getString(mapIn, g + "GroupName"));
				group.setGroupId(QueryUtil.getString(mapIn, g + "GroupId"));
				//groups.add(group);
				
				cidrList.add(QueryUtil.getString(mapIn, s + n + ".IpRanges." + m + ".CidrIp"));
			}
			permission.setIpRanges(cidrList);
			permission.setUserIdGroupPairs(groups);
			ipPermissions.add(permission);
		}
		validateIpPermissions(ipPermissions);
		return ipPermissions;
	}*/

	/*//Validation for the IpPermissions gotten from the map, mostly checking for missing parameters or bad parameter combinations
	private static void validateIpPermissions(final List<IpPermission> ipPermissions) {
		{
			int n = 0;
			for(IpPermission ip : ipPermissions){
				if(ip.getIpProtocol() == null || "".equals(ip.getIpProtocol())){
					throw QueryFaults.MissingParameter("IpPermissions." + (n+1) + ".IpProtocol is required");
				}
				//Call above specified that -2 would be default value if there was no value for fromPort or toPort
				else if(ip.getFromPort() == -2){
					throw QueryFaults.MissingParameter("IpPermissions." + (n+1) + ".FromPort is required");
				}
				else if(ip.getToPort() == -2){
					throw QueryFaults.MissingParameter("IpPermissions." + (n+1) + ".ToPort is required");
				}
				final List<UserIdGroupPair> groups = ip.getUserIdGroupPairs();
				final List<String> ipRanges = ip.getIpRanges();
				int m = 0;
				for(UserIdGroupPair group : groups){
					//check if neither IpPermissions.n.Groups.m and IpPermissions.n.IpRanges.m were specified
					if(group == null && ipRanges.get(m) == null){
						throw QueryFaults.MissingParameter("Must specify either a CidrIp or a Group name/Id for each IpPermission.");
					}
					if(!groupIsEmpty(group)){
						//check if IpPermissions.n.Groups.m and IpPermissions.n.IpRanges.m were both given
						if(ipRanges.get(m)!= null){
							throw QueryFaults.InvalidParameterCombination("Cannot specify a Group and a Cidrip for the same IpPermission.");
						}
						//check if IpPermissions.n.Groups.m.groupId and IpPermissions.n.Groups.m.groupName were specified together
						else if((group.getGroupId() !=null && !"".equals(group.getGroupId())) && 
								(group.getGroupName()!= null && !"".equals(group.getGroupName()))){
							throw QueryFaults.InvalidParameterCombination("GroupId and GroupName should not be specified together for any IpPermission group");
						}
						else if(group.getUserId() == null){
							throw QueryFaults.MissingParameter("UserId must be specified for each IpPermissions group given.");
						}
					}
					else if(ipRanges.get(m) !=null){
						//TODO:check for valid CidrIp range
					}
					m++;
				}
				n++;
			}
		}
	}*/
	
	//gets a group id from a group name
	public static String getFirewallId(final FirewallSupport fs, final String groupName) throws Exception {
		String firewallId= "";
		for(Firewall f : fs.list()){
			if(f.getName().equals(groupName)){
				firewallId = f.getProviderFirewallId();
				break;
			}
		}
		return firewallId;
	}

	/*//Group won't ever be null, its elements inside will be null
	public static boolean groupIsEmpty(UserIdGroupPair g){
		return g == null || (g.getGroupId() == null && g.getGroupName() == null && g.getUserId() == null);
	}*/
}
