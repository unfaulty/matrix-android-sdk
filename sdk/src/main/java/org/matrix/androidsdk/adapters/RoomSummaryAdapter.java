/*
 * Copyright 2015 OpenMarket Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.androidsdk.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.matrix.androidsdk.MXSession;
import org.matrix.androidsdk.R;
import org.matrix.androidsdk.data.Room;
import org.matrix.androidsdk.data.RoomState;
import org.matrix.androidsdk.data.RoomSummary;
import org.matrix.androidsdk.rest.model.Event;
import org.matrix.androidsdk.rest.model.PublicRoom;
import org.matrix.androidsdk.util.EventDisplay;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public abstract class RoomSummaryAdapter extends BaseExpandableListAdapter {

    protected Context mContext;
    private LayoutInflater mLayoutInflater;
    private int mLayoutResourceId;
    private int mHeaderLayoutResourceId;

    private int mUnreadColor;
    private int mHighlightColor;
    private int mPublicHighlightColor;

    private ArrayList<ArrayList<RoomSummary>> mRecentsSummariesList;
    private List<PublicRoom>mPublicRoomsList;

    public int mPublicsGroupIndex = -1;

    private  boolean mDisplayAllGroups = true;

    private ArrayList<ArrayList<RoomSummary>> mFilteredRecentsSummariesList;
    private List<PublicRoom>mFilteredPublicRoomsList;

    private String mSearchedPattern = "";

    private ArrayList<String> mHighLightedRooms = new ArrayList<String>();
    private ArrayList<HashMap<String, Integer>> mUnreadCountMaps = new ArrayList<HashMap<String, Integer>>();

    // abstract methods
    public abstract int getUnreadMessageBackgroundColor();
    public abstract int getHighlightMessageBackgroundColor();
    public abstract int getPublicHighlightMessageBackgroundColor();
    public abstract boolean displayPublicRooms();
    public abstract String myRoomsTitle(int section);
    public abstract String publicRoomsTitle();

    /**
     * Construct an adapter which will display a list of rooms.
     * @param context Activity context
     * @param layoutResourceId The resource ID of the layout for each item. Must have TextViews with
     *                         the IDs: roomsAdapter_roomName, roomsAdapter_roomTopic
     * @param headerLayoutResourceId the header layout id
     */
    public RoomSummaryAdapter(Context context, int nbrSections, int layoutResourceId, int headerLayoutResourceId) {
        mContext = context;
        mLayoutResourceId = layoutResourceId;
        mHeaderLayoutResourceId = headerLayoutResourceId;
        mLayoutInflater = LayoutInflater.from(mContext);
        //setNotifyOnChange(false);

        mRecentsSummariesList = new ArrayList<ArrayList<RoomSummary>>();
        for(int section = 0; section < nbrSections; section++) {
            mRecentsSummariesList.add(new ArrayList<RoomSummary>());
            mUnreadCountMaps.add(new HashMap<String, Integer>());
        }

        mPublicRoomsList  = new ArrayList<PublicRoom>();
        mUnreadColor = getUnreadMessageBackgroundColor();
        mHighlightColor = getHighlightMessageBackgroundColor();
        mPublicHighlightColor = getPublicHighlightMessageBackgroundColor();
    }

    /**
     *  unread messages map
     */
    public ArrayList<HashMap<String, Integer>> getUnreadCountMap() {
        return mUnreadCountMaps;
    }

    public void setUnreadCountMap(ArrayList<HashMap<String, Integer>> maps) {
        mUnreadCountMaps = maps;
    }

    /**
     *  search management
     */
    public void setSearchedPattern(String pattern) {
        if (null == pattern) {
            pattern = "";
        }

        if (!pattern.equals(mSearchedPattern)) {
            mSearchedPattern = pattern.toLowerCase();
            this.notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        mFilteredRecentsSummariesList = new ArrayList<ArrayList<RoomSummary>>();
        mFilteredPublicRoomsList = new ArrayList<PublicRoom>();

        // there is a pattern to search
        if (mSearchedPattern.length() > 0) {

            for(int index = 0; index < mRecentsSummariesList.size(); index++) {
                ArrayList<RoomSummary> roomSummaries = mRecentsSummariesList.get(index);
                ArrayList<RoomSummary> filteredRes = new ArrayList<RoomSummary>();

                // search in the recent rooms
                for (RoomSummary summary : roomSummaries) {
                    String roomName = summary.getRoomName();

                    if (!TextUtils.isEmpty(roomName) && (roomName.toLowerCase().indexOf(mSearchedPattern) >= 0)) {
                        filteredRes.add(summary);
                    } else {
                        String topic = summary.getRoomTopic();

                        if (!TextUtils.isEmpty(topic) && (topic.toLowerCase().indexOf(mSearchedPattern) >= 0)) {
                            filteredRes.add(summary);
                        }
                    }
                }
                mFilteredRecentsSummariesList.add(filteredRes);
            }

            for (PublicRoom publicRoom : mPublicRoomsList) {
                String roomName = publicRoom.name;

                if (!TextUtils.isEmpty(roomName) && (roomName.toLowerCase().indexOf(mSearchedPattern) >= 0)) {
                    mFilteredPublicRoomsList.add(publicRoom);
                } else {
                    String alias = publicRoom.roomAliasName;

                    if (!TextUtils.isEmpty(alias) && (alias.toLowerCase().indexOf(mSearchedPattern) >= 0)) {
                        mFilteredPublicRoomsList.add(publicRoom);
                    }
                }
            }
        }

        super.notifyDataSetChanged();
    }

    /**
     * Check if the group index is the recents one.
     * @param groupIndex the group index.
     * @return true if the recents group oone
     */
    public boolean isRecentsGroupIndex(int groupIndex) {
        return groupIndex != mPublicsGroupIndex;
    }

    /**
     * Check if the group index is the public ones.
     * @param groupIndex the group index.
     * @return true if the group is the publics one.
     */
    public boolean isPublicsGroupIndex(int groupIndex) {
        return groupIndex == mPublicsGroupIndex;
    }

    /**
     * Force to display all the groups
     * @param displayAllGroups status
     */
    public void setDisplayAllGroups(boolean displayAllGroups) {
        displayAllGroups |= displayPublicRooms();

        if (mDisplayAllGroups != displayAllGroups) {
            mDisplayAllGroups = displayAllGroups;
            notifyDataSetChanged();
        }
    }

    /**
     * public rooms list management
     */
    public void setPublicRoomsList(List<PublicRoom> aRoomsList) {
        if (null == aRoomsList) {
            mPublicRoomsList  = new ArrayList<PublicRoom>();
        } else {
            mPublicRoomsList = aRoomsList;
            sortSummaries();
        }

        this.notifyDataSetChanged();
    }

    public PublicRoom getPublicRoomAt(int index) {
        if (mSearchedPattern.length() > 0) {
            return mFilteredPublicRoomsList.get(index);
        } else {
            return mPublicRoomsList.get(index);
        }
    }
    /**
     * recent rooms list management
     */
    public ArrayList<ArrayList<RoomSummary>> getRecentsSummariesList() {
        return mRecentsSummariesList;
    }

    public void addRoomSummary(int section, RoomSummary roomSummary) {
        if (section < mRecentsSummariesList.size()) {

            ArrayList<RoomSummary> list = mRecentsSummariesList.get(section);

            // check if the summary is not added twice.
            for (RoomSummary rSum : list) {
                if (rSum.getRoomId().equals(roomSummary.getRoomId())) {
                    return;
                }
            }

            list.add(roomSummary);
        }
    }

    public RoomSummary getRoomSummaryAt(int section, int index) {
        if (mSearchedPattern.length() > 0) {
            return mFilteredRecentsSummariesList.get(section).get(index);
        } else {
            return mRecentsSummariesList.get(section).get(index);
        }
    }

    public void removeRoomSummary(int section, RoomSummary roomSummary) {
        mRecentsSummariesList.get(section).remove(roomSummary);

        if (null != roomSummary.getRoomId()) {
            mUnreadCountMaps.get(section).remove(roomSummary.getRoomId());
        }
    }

    public RoomSummary getSummaryByRoomId(int section, String roomId) {
        ArrayList<RoomSummary> list = mRecentsSummariesList.get(section);

        for (int i=0; i< list.size(); i++) {
            RoomSummary summary = list.get(i);
            if (roomId.equals(summary.getRoomId())) {
                return summary;
            }
        }
        return null;
    }

    public void removeSection(int section) {
        mRecentsSummariesList.remove(section);

        if ((null != mFilteredRecentsSummariesList) && (mFilteredRecentsSummariesList.size() > section)) {
            mFilteredRecentsSummariesList.remove(section);
        }

        mUnreadCountMaps.remove(section);
    }

    /**
     * Set the latest event for a room summary.
     * @param event The latest event
     * @param roomState the roomState
     */
    public void setLatestEvent(int section, Event event, RoomState roomState) {
        RoomSummary summary = getSummaryByRoomId(section, event.roomId);
        if (summary != null) {
            summary.setLatestEvent(event);
            summary.setLatestRoomState(roomState);
            sortSummaries();
            notifyDataSetChanged();
        }
    }

    /**
     * Increments the unread message counters for a dedicated room.
     * @param roomId The room identifier
     */
    public void incrementUnreadCount(int section, String roomId) {
        Integer count = mUnreadCountMaps.get(section).get(roomId);
        if (count == null) {
            count = 0;
        }
        mUnreadCountMaps.get(section).put(roomId, count + 1);
    }

    /**
     * Defines that the room must be highlighted in the rooms list
     * @param roomId The room ID of the room to highlight.
     */
    public void highlightRoom(String roomId) {
        if (mHighLightedRooms.indexOf(roomId) < 0) {
            mHighLightedRooms.add(roomId);
        }
    }

    /**
     * Reset the unread messages counter and remove the rooms from the highlighted rooms lists.
     * @param roomId
     */
    public void resetUnreadCount(int section, String roomId) {
        mUnreadCountMaps.get(section).put(roomId, 0);
        mHighLightedRooms.remove(roomId);
    }


    /**
     * Reset the unread message counters.
     */
    public void resetUnreadCounts() {
        for(int section = 0; section < mRecentsSummariesList.size(); section++) {
            resetUnreadCounts(section);
        }
    }

    /**
     * Reset the unread message counters.
     */
    public void resetUnreadCounts(int section) {
        Set<String> roomIds = mUnreadCountMaps.get(section).keySet();

        for(String roomId : roomIds) {
            resetUnreadCount(section, roomId);
        }
    }

    /**
     * Sort the room summaries list.
     * 1 - Sort by the latest event timestamp (most recent first).
     * 2 - Sort the public rooms by the number of members (bigger room first)
     */
    public void sortSummaries() {
        for(int section = 0; section < mRecentsSummariesList.size(); section++) {
            ArrayList<RoomSummary> summariesList = mRecentsSummariesList.get(section);

            Collections.sort(summariesList, new Comparator<RoomSummary>() {
                @Override
                public int compare(RoomSummary lhs, RoomSummary rhs) {
                    if (lhs == null || lhs.getLatestEvent() == null) {
                        return 1;
                    } else if (rhs == null || rhs.getLatestEvent() == null) {
                        return -1;
                    }

                    if (lhs.getLatestEvent().getOriginServerTs() > rhs.getLatestEvent().getOriginServerTs()) {
                        return -1;
                    } else if (lhs.getLatestEvent().getOriginServerTs() < rhs.getLatestEvent().getOriginServerTs()) {
                        return 1;
                    }
                    return 0;
                }
            });
        }

        Collections.sort(mPublicRoomsList, new Comparator<PublicRoom>() {
            @Override
            public int compare(PublicRoom publicRoom, PublicRoom publicRoom2) {
                return publicRoom2.numJoinedMembers - publicRoom.numJoinedMembers;
            }
        });
    }

    public abstract Room roomFromRoomSummary(RoomSummary roomSummary);

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        // display a spinner while loading the public rooms
        // detect if the view is progressbar_waiting_room_members one
        View spinner = null;
        if (null != convertView) {
            spinner = convertView.findViewById(R.id.progressbar_waiting_room_members);
        }

        // assume that some public rooms are defined
        if ((groupPosition == mPublicsGroupIndex) && (0 == mPublicRoomsList.size())) {
            if (null == spinner) {
                convertView = mLayoutInflater.inflate(R.layout.adapter_item_waiting_room_members, parent, false);
            }
            return convertView;
        }

        // must not reuse the view if it is not the right type
        if (null != spinner) {
            convertView = null;
        }

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(mLayoutResourceId, parent, false);
        }

        // default UI
        // when a room is deleting, the UI is dimmed
        final View deleteProgress = (View) convertView.findViewById(R.id.roomSummaryAdapter_delete_progress);
        deleteProgress.setVisibility(View.GONE);
        convertView.setAlpha(1.0f);

        if (isRecentsGroupIndex(groupPosition)) {
            List<RoomSummary> summariesList = (mSearchedPattern.length() > 0) ? mFilteredRecentsSummariesList.get(groupPosition) : mRecentsSummariesList.get(groupPosition);

            RoomSummary summary = (childPosition < summariesList.size()) ? summariesList.get(childPosition) : summariesList.get(summariesList.size() - 1);

            Integer unreadCount = mUnreadCountMaps.get(groupPosition).get(summary.getRoomId());

            if ((unreadCount == null) || (unreadCount == 0)) {
                convertView.setBackgroundColor(0);
            } else if (mHighLightedRooms.indexOf(summary.getRoomId()) >= 0) {
                convertView.setBackgroundColor(mHighlightColor);
            } else {
                convertView.setBackgroundColor(mUnreadColor);
            }

            CharSequence message = summary.getRoomTopic();
            String timestamp = null;

            TextView textView = (TextView) convertView.findViewById(R.id.roomSummaryAdapter_roomName);

            RoomState latestRoomState = summary.getLatestRoomState();
            if (null == latestRoomState) {
                Room room = roomFromRoomSummary(summary);

                if ((null != room) && (null != room.getLiveState())) {
                    latestRoomState = room.getLiveState().deepCopy();
                }
            }

            // the public rooms are displayed with bold fonts
            if ((null != latestRoomState) && (null != latestRoomState.visibility) && latestRoomState.visibility.equals(RoomState.VISIBILITY_PUBLIC)) {
                textView.setTypeface(null, Typeface.BOLD);
            } else {
                textView.setTypeface(null, Typeface.NORMAL);
            }

            // display the unread messages count
            String roomNameMessage = (latestRoomState != null) ? latestRoomState.getDisplayName(summary.getMatrixId()) : summary.getRoomName();

            if (null != roomNameMessage) {
                if ((null != unreadCount) && (unreadCount > 0)) {
                    roomNameMessage += " (" + unreadCount + ")";
                }
            }

            textView.setText(roomNameMessage);

            if (summary.getLatestEvent() != null) {
                EventDisplay display = new EventDisplay(mContext, summary.getLatestEvent(), latestRoomState);
                display.setPrependMessagesWithAuthor(true);
                message = display.getTextualDisplay();
                timestamp = summary.getLatestEvent().formattedOriginServerTs();
            }

            // check if this is an invite
            if (summary.isInvited()) {
                String memberName = summary.getInviterUserId();

                if (null != latestRoomState) {
                    memberName = latestRoomState.getMemberName(memberName);
                }

                message = mContext.getString(R.string.summary_user_invitation, memberName);
            }

            textView = (TextView) convertView.findViewById(R.id.roomSummaryAdapter_message);
            textView.setText(message);
            textView = (TextView) convertView.findViewById(R.id.roomSummaryAdapter_ts);
            textView.setVisibility(View.VISIBLE);
            textView.setText(timestamp);

            Room room = roomFromRoomSummary(summary);

            if ((null != room) && room.isLeaving()) {
                convertView.setAlpha(0.3f);
                deleteProgress.setVisibility(View.VISIBLE);
            }

        } else {
            List<PublicRoom> publicRoomsList = (mSearchedPattern.length() > 0) ? mFilteredPublicRoomsList : mPublicRoomsList;
            PublicRoom publicRoom = publicRoomsList.get(childPosition);

            String matrixId = null;

            if ((mRecentsSummariesList.size() > 0) && (mRecentsSummariesList.get(0).size() > 0)) {
                matrixId = mRecentsSummariesList.get(0).get(0).getMatrixId();
            }

            String displayName = publicRoom.getDisplayName(matrixId);

            TextView textView = (TextView) convertView.findViewById(R.id.roomSummaryAdapter_roomName);
            textView.setTypeface(null, Typeface.BOLD);
            textView.setText(displayName);

            textView = (TextView) convertView.findViewById(R.id.roomSummaryAdapter_message);
            textView.setText(publicRoom.topic);

            textView = (TextView) convertView.findViewById(R.id.roomSummaryAdapter_ts);
            textView.setVisibility(View.VISIBLE);

            if (publicRoom.numJoinedMembers > 1) {
                textView.setText(publicRoom.numJoinedMembers + " " + mContext.getString(R.string.users));
            } else {
                textView.setText(publicRoom.numJoinedMembers + " " + mContext.getString(R.string.user));
            }

            String alias = publicRoom.getFirstAlias();

            if ((null != alias) && (mHighLightedRooms.indexOf(alias) >= 0)) {
                convertView.setBackgroundColor(mPublicHighlightColor);
            } else {
                convertView.setBackgroundColor(0);
            }
        }

        return convertView;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(mHeaderLayoutResourceId, null);
        }

        TextView heading = (TextView) convertView.findViewById(R.id.heading);

        if (isRecentsGroupIndex(groupPosition)) {

            int unreadCount = 0;

            for(Integer i : mUnreadCountMaps.get(groupPosition).values()) {
                unreadCount += i;
            }

            String header = myRoomsTitle(groupPosition);

            if (unreadCount > 0) {
                header += " ("  + unreadCount + ")";
            }

            heading.setText(header);
        } else {
            heading.setText(publicRoomsTitle());
        }

        ImageView imageView = (ImageView) convertView.findViewById(R.id.heading_image);

        if (isExpanded) {
            imageView.setImageResource(R.drawable.expander_close_holo_light);
        } else {
            imageView.setImageResource(R.drawable.expander_open_holo_light);
        }

        return convertView;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (isRecentsGroupIndex(groupPosition)) {
            ArrayList<ArrayList<RoomSummary>> list = (mSearchedPattern.length() > 0) ? mFilteredRecentsSummariesList : mRecentsSummariesList;

            if ((null == list) || (list.size() <= groupPosition)) {
                return 0;
            } else {
                return list.get(groupPosition).size();
            }
        } else {
            // display a spinner until the public rooms are loaded
            if (mPublicRoomsList.size() == 0) {
                return 1;
            } else {
                return (mSearchedPattern.length() > 0) ? mFilteredPublicRoomsList.size() : mPublicRoomsList.size();
            }
        }
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public int getGroupCount() {
        int count = 0;

        mPublicsGroupIndex = -1;

        count += mRecentsSummariesList.size();

        // display the public rooms in the recents only if there is no dedicated room
        if ((mRecentsSummariesList.size() == 0) || mDisplayAllGroups) {
            mPublicsGroupIndex = count;
            count++;
        }

        return count;
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
