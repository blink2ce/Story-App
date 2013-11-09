package com.example.tictactoe;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class Group {

	public final String groupName;
	public final int groupMemberCnt;
	public final int groupMemberMax;
	
	public Group(String groupName, int groupMemberCnt, int groupMemberMax) {
		this.groupName = groupName;
		this.groupMemberCnt = groupMemberCnt;
		this.groupMemberMax = groupMemberMax;
	}

	
	/**
	 * Create a list of Group objects from a comma-separated list of group descriptions.
	 * 
	 * @param groupStringList string containing comma-separated group descriptions, e.g. "@group1(1/10),@group2(3/3)"
	 * @return list containing group objects
	 */
	public static List<Group> createGroups(String groupStringList) {
		
		ArrayList<Group> rval = new ArrayList<Group>();
		// @groupname(membercnt/maxcnt),@groupname(membercnt/maxcnt),...
		String[] groups = groupStringList.split(",");
		for (String groupString : groups)
			rval.add(Group.createGroup(groupString));		
		return rval;
	}

	/**
	 * Create a Group object from a group description string.
	 * 
	 * @param groupString group description, e.g. "@group1(1/10)"
	 * @return the Group object
	 */
	public static Group createGroup(String groupString) {
		// @groupname(membercnt/maxcnt)
		String[] tokens3 = groupString.split("\\(|/|\\)");
		String groupName = tokens3[0];
		int groupMemberCnt = Integer.parseInt(tokens3[1]);
		int groupMemberMax = Integer.parseInt(tokens3[2]);
		return new Group(groupName, groupMemberCnt, groupMemberMax);
	}
	
}