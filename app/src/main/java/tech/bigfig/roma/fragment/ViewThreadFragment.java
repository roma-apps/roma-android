/* Copyright 2017 Andrew Dawson
 *
 * This file is a part of Roma.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Roma is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Roma; if not,
 * see <http://www.gnu.org/licenses>. */

package tech.bigfig.roma.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;

import tech.bigfig.roma.AccountListActivity;
import tech.bigfig.roma.BaseActivity;
import tech.bigfig.roma.BuildConfig;
import tech.bigfig.roma.R;
import tech.bigfig.roma.ViewThreadActivity;
import tech.bigfig.roma.adapter.ThreadAdapter;
import tech.bigfig.roma.appstore.BlockEvent;
import tech.bigfig.roma.appstore.EventHub;
import tech.bigfig.roma.appstore.FavouriteEvent;
import tech.bigfig.roma.appstore.ReblogEvent;
import tech.bigfig.roma.appstore.StatusComposedEvent;
import tech.bigfig.roma.appstore.StatusDeletedEvent;
import tech.bigfig.roma.di.Injectable;
import tech.bigfig.roma.entity.Card;
import tech.bigfig.roma.entity.Poll;
import tech.bigfig.roma.entity.Status;
import tech.bigfig.roma.entity.StatusContext;
import tech.bigfig.roma.interfaces.StatusActionListener;
import tech.bigfig.roma.network.MastodonApi;
import tech.bigfig.roma.network.TimelineCases;
import tech.bigfig.roma.util.ListStatusAccessibilityDelegate;
import tech.bigfig.roma.util.PairedList;
import tech.bigfig.roma.util.SmartLengthInputFilter;
import tech.bigfig.roma.util.ThemeUtils;
import tech.bigfig.roma.util.ViewDataUtils;
import tech.bigfig.roma.view.ConversationLineItemDecoration;
import tech.bigfig.roma.viewdata.NotificationViewData;
import tech.bigfig.roma.viewdata.StatusViewData;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.util.Pair;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import io.reactivex.android.schedulers.AndroidSchedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.uber.autodispose.AutoDispose.autoDisposable;
import static com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider.from;

public final class ViewThreadFragment extends SFragment implements
        SwipeRefreshLayout.OnRefreshListener, StatusActionListener, Injectable {
    private static final String TAG = "ViewThreadFragment";

    @Inject
    public MastodonApi mastodonApi;
    @Inject
    public EventHub eventHub;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ThreadAdapter adapter;
    private String thisThreadsStatusId;
    private boolean alwaysShowSensitiveMedia;

    private int statusIndex = 0;

    private final PairedList<Status, StatusViewData.Concrete> statuses =
            new PairedList<>(new Function<Status, StatusViewData.Concrete>() {
                @Override
                public StatusViewData.Concrete apply(Status input) {
                    return ViewDataUtils.statusToViewData(
                            input,
                            alwaysShowSensitiveMedia
                    );
                }
            });

    public static ViewThreadFragment newInstance(String id) {
        Bundle arguments = new Bundle(1);
        ViewThreadFragment fragment = new ViewThreadFragment();
        arguments.putString("id", id);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        thisThreadsStatusId = getArguments().getString("id");

        adapter = new ThreadAdapter(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_view_thread, container, false);

        Context context = getContext();
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.roma_blue);
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ThemeUtils.getColor(context, android.R.attr.colorBackground));

        recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAccessibilityDelegateCompat(
                new ListStatusAccessibilityDelegate(recyclerView, this, statuses::getPairedItemOrNull));
        DividerItemDecoration divider = new DividerItemDecoration(
                context, layoutManager.getOrientation());
        recyclerView.addItemDecoration(divider);

        recyclerView.addItemDecoration(new ConversationLineItemDecoration(context));
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(
                getActivity());
        alwaysShowSensitiveMedia = accountManager.getActiveAccount().getAlwaysShowSensitiveMedia();
        boolean mediaPreviewEnabled = accountManager.getActiveAccount().getMediaPreviewEnabled();
        adapter.setMediaPreviewEnabled(mediaPreviewEnabled);
        boolean useAbsoluteTime = preferences.getBoolean("absoluteTimeView", false);
        adapter.setUseAbsoluteTime(useAbsoluteTime);
        boolean animateAvatars = preferences.getBoolean("animateGifAvatars", false);
        adapter.setAnimateAvatar(animateAvatars);

        recyclerView.setAdapter(adapter);

        statuses.clear();

        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        return rootView;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        onRefresh();

        eventHub.getEvents()
                .observeOn(AndroidSchedulers.mainThread())
                .as(autoDisposable(from(this, Lifecycle.Event.ON_DESTROY)))
                .subscribe(event -> {
                    if (event instanceof FavouriteEvent) {
                        handleFavEvent((FavouriteEvent) event);
                    } else if (event instanceof ReblogEvent) {
                        handleReblogEvent((ReblogEvent) event);
                    } else if (event instanceof BlockEvent) {
                        removeAllByAccountId(((BlockEvent) event).getAccountId());
                    } else if (event instanceof StatusComposedEvent) {
                        handleStatusComposedEvent((StatusComposedEvent) event);
                    } else if (event instanceof StatusDeletedEvent) {
                        handleStatusDeletedEvent((StatusDeletedEvent) event);
                    }
                });
    }

    public void onRevealPressed() {
        boolean allExpanded = allExpanded();
        for (int i = 0; i < statuses.size(); i++) {
            StatusViewData.Concrete newViewData =
                    new StatusViewData.Concrete.Builder(statuses.getPairedItem(i))
                            .setIsExpanded(!allExpanded)
                            .createStatusViewData();
            statuses.setPairedItem(i, newViewData);
        }
        adapter.setStatuses(statuses.getPairedCopy());
    }

    private boolean allExpanded() {
        boolean allExpanded = true;
        for (int i = 0; i < statuses.size(); i++) {
            if (!statuses.getPairedItem(i).isExpanded()) {
                allExpanded = false;
                break;
            }
        }
        return allExpanded;
    }

    @Override
    public void onRefresh() {
        sendStatusRequest(thisThreadsStatusId);
        sendThreadRequest(thisThreadsStatusId);
    }

    @Override
    public void onReply(int position) {
        super.reply(statuses.get(position));
    }

    @Override
    public void onReblog(final boolean reblog, final int position) {
        final Status status = statuses.get(position);

        timelineCases.reblog(statuses.get(position), reblog)
                .observeOn(AndroidSchedulers.mainThread())
                .as(autoDisposable(from(this)))
                .subscribe(
                        (newStatus) -> updateStatus(position, newStatus),
                        (t) -> Log.d(getClass().getSimpleName(),
                                "Failed to reblog status: " + status.getId(), t)
                );
    }

    @Override
    public void onFavourite(final boolean favourite, final int position) {
        final Status status = statuses.get(position);

        timelineCases.favourite(statuses.get(position), favourite)
                .observeOn(AndroidSchedulers.mainThread())
                .as(autoDisposable(from(this)))
                .subscribe(
                        (newStatus) -> updateStatus(position, newStatus),
                        (t) -> Log.d(getClass().getSimpleName(),
                                "Failed to favourite status: " + status.getId(), t)
                );
    }

    private void updateStatus(int position, Status status) {
        if (position >= 0 && position < statuses.size()) {

            Status actionableStatus = status.getActionableStatus();

            StatusViewData.Concrete viewData = new StatusViewData.Builder(statuses.getPairedItem(position))
                    .setReblogged(actionableStatus.getReblogged())
                    .setReblogsCount(actionableStatus.getReblogsCount())
                    .setFavourited(actionableStatus.getFavourited())
                    .setFavouritesCount(actionableStatus.getFavouritesCount())
                    .setRepliesCount(actionableStatus.getRepliesCount())
                    .createStatusViewData();
            statuses.setPairedItem(position, viewData);

            adapter.setItem(position, viewData, true);

        }
    }

    @Override
    public void onMore(@NonNull View view, int position) {
        super.more(statuses.get(position), view, position);
    }

    @Override
    public void onViewMedia(int position, int attachmentIndex, @NonNull View view) {
        Status status = statuses.get(position);
        super.viewMedia(attachmentIndex, status, view);
    }

    @Override
    public void onViewThread(int position) {
        Status status = statuses.get(position);
        if (thisThreadsStatusId.equals(status.getId())) {
            // If already viewing this thread, don't reopen it.
            return;
        }
        super.viewThread(status);
    }

    @Override
    public void onOpenReblog(int position) {
        // there should be no reblogs in the thread but let's implement it to be sure
        super.openReblog(statuses.get(position));
    }

    @Override
    public void onExpandedChange(boolean expanded, int position) {
        StatusViewData.Concrete newViewData =
                new StatusViewData.Builder(statuses.getPairedItem(position))
                        .setIsExpanded(expanded)
                        .createStatusViewData();
        statuses.setPairedItem(position, newViewData);
        adapter.setItem(position, newViewData, false);
        updateRevealIcon();
    }

    @Override
    public void onContentHiddenChange(boolean isShowing, int position) {
        StatusViewData.Concrete newViewData =
                new StatusViewData.Builder(statuses.getPairedItem(position))
                        .setIsShowingSensitiveContent(isShowing)
                        .createStatusViewData();
        statuses.setPairedItem(position, newViewData);
        adapter.setItem(position, newViewData, false);
    }

    @Override
    public void onLoadMore(int position) {

    }

    @Override
    public void onShowReblogs(int position) {
        String statusId = statuses.get(position).getId();
        Intent intent = AccountListActivity.newIntent(getContext(), AccountListActivity.Type.REBLOGGED, statusId);
        ((BaseActivity) getActivity()).startActivityWithSlideInAnimation(intent);
    }

    @Override
    public void onShowFavs(int position) {
        String statusId = statuses.get(position).getId();
        Intent intent = AccountListActivity.newIntent(getContext(), AccountListActivity.Type.FAVOURITED, statusId);
        ((BaseActivity) getActivity()).startActivityWithSlideInAnimation(intent);
    }

    @Override
    public void onContentCollapsedChange(boolean isCollapsed, int position) {
        if (position < 0 || position >= statuses.size()) {
            Log.e(TAG, String.format("Tried to access out of bounds status position: %d of %d", position, statuses.size() - 1));
            return;
        }

        StatusViewData.Concrete status = statuses.getPairedItem(position);
        if (status == null) {
            // Statuses PairedList contains a base type of StatusViewData.Concrete and also doesn't
            // check for null values when adding values to it although this doesn't seem to be an issue.
            Log.e(TAG, String.format(
                    "Expected StatusViewData.Concrete, got null instead at position: %d of %d",
                    position,
                    statuses.size() - 1
            ));
            return;
        }

        StatusViewData.Concrete updatedStatus = new StatusViewData.Builder(status)
                .setCollapsible(!SmartLengthInputFilter.hasBadRatio(
                        status.getContent(),
                        SmartLengthInputFilter.LENGTH_DEFAULT
                ))
                .setCollapsed(isCollapsed)
                .createStatusViewData();
        statuses.setPairedItem(position, updatedStatus);
        recyclerView.post(() -> adapter.setItem(position, updatedStatus, true));
    }

    @Override
    public void onViewTag(String tag) {
        super.viewTag(tag);
    }

    @Override
    public void onViewAccount(String id) {
        super.viewAccount(id);
    }

    @Override
    public void removeItem(int position) {
        if (position == statusIndex) {
            //the status got removed, close the activity
            getActivity().finish();
        }
        Status status = statuses.get(position);
        replyDeleted(status.getId());

        /*if (statuses.remove(status))
            adapter.setStatuses(statuses.getPairedCopy());*/
    }

    public void onVoteInPoll(int position, @NonNull List<Integer> choices) {
        final Status status = statuses.get(position).getActionableStatus();

        setVoteForPoll(position, status.getPoll().votedCopy(choices));

        timelineCases.voteInPoll(status, choices)
                .observeOn(AndroidSchedulers.mainThread())
                .as(autoDisposable(from(this)))
                .subscribe(
                        (newPoll) -> setVoteForPoll(position, newPoll),
                        (t) -> Log.d(TAG,
                                "Failed to vote in poll: " + status.getId(), t)
                );

    }

    private void setVoteForPoll(int position, Poll newPoll) {

        StatusViewData.Concrete viewData = statuses.getPairedItem(position);

        StatusViewData.Concrete newViewData = new StatusViewData.Builder(viewData)
                .setPoll(newPoll)
                .createStatusViewData();
        statuses.setPairedItem(position, newViewData);
        adapter.setItem(position, newViewData, true);
    }

    private void removeAllByAccountId(String accountId) {
        Status status = null;
        if (!statuses.isEmpty()) {
            status = statuses.get(statusIndex);
        }
        // using iterator to safely remove items while iterating
        Iterator<Status> iterator = statuses.iterator();
        while (iterator.hasNext()) {
            Status s = iterator.next();
            if (s.getAccount().getId().equals(accountId) || s.getActionableStatus().getAccount().getId().equals(accountId)) {
                iterator.remove();
            }
        }
        statusIndex = statuses.indexOf(status);
        if (statusIndex == -1) {
            //the status got removed, close the activity
            getActivity().finish();
            return;
        }
        adapter.setDetailedStatusPosition(statusIndex);
        adapter.setStatuses(statuses.getPairedCopy());
    }

    private void sendStatusRequest(final String id) {
        Call<Status> call = mastodonApi.status(id);
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful()) {
                    int position = setStatus(response.body());
                    recyclerView.scrollToPosition(position);
                } else {
                    onThreadRequestFailure(id);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                onThreadRequestFailure(id);
            }
        });
        callList.add(call);
    }

    private void sendThreadRequest(final String id) {
        Call<StatusContext> call = mastodonApi.statusContext(id);
        call.enqueue(new Callback<StatusContext>() {
            @Override
            public void onResponse(@NonNull Call<StatusContext> call, @NonNull Response<StatusContext> response) {
                StatusContext context = response.body();
                if (response.isSuccessful() && context != null) {
                    swipeRefreshLayout.setRefreshing(false);
                    setContext(context.getAncestors(), context.getDescendants());
                } else {
                    onThreadRequestFailure(id);
                }
            }

            @Override
            public void onFailure(@NonNull Call<StatusContext> call, @NonNull Throwable t) {
                onThreadRequestFailure(id);
            }
        });
        callList.add(call);
    }

    private void onThreadRequestFailure(final String id) {
        View view = getView();
        swipeRefreshLayout.setRefreshing(false);
        if (view != null) {
            Snackbar.make(view, R.string.error_generic, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_retry, v -> {
                        sendThreadRequest(id);
                        sendStatusRequest(id);
                    })
                    .show();
        } else {
            Log.e(TAG, "Couldn't display thread fetch error message");
        }
    }

    private int setStatus(Status status) {
        if (statuses.size() > 0
                && statusIndex < statuses.size()
                && statuses.get(statusIndex).equals(status)) {
            // Do not add this status on refresh, it's already in there.
            statuses.set(statusIndex, status);
            return statusIndex;
        }
        int i = statusIndex;
        statuses.add(i, status);
        adapter.setDetailedStatusPosition(i);
        adapter.addItem(i, statuses.getPairedItem(i));
        updateRevealIcon();
        return i;
    }

    private void setContext(List<Status> ancestors, List<Status> descendants) {
        Status mainStatus = null;

        // In case of refresh, remove old ancestors and descendants first. We'll remove all blindly,
        // as we have no guarantee on their order to be the same as before
        int oldSize = statuses.size();
        if (oldSize > 1) {
            mainStatus = statuses.get(statusIndex);
            statuses.clear();
            adapter.clearItems();
        }

        // Insert newly fetched ancestors
        statusIndex = ancestors.size();
        adapter.setDetailedStatusPosition(statusIndex);
        statuses.addAll(0, ancestors);
        List<StatusViewData.Concrete> ancestorsViewDatas = statuses.getPairedCopy().subList(0, statusIndex);
        if (BuildConfig.DEBUG && ancestors.size() != ancestorsViewDatas.size()) {
            String error = String.format(Locale.getDefault(),
                    "Incorrectly got statusViewData sublist." +
                            " ancestors.size == %d ancestorsViewDatas.size == %d," +
                            " statuses.size == %d",
                    ancestors.size(), ancestorsViewDatas.size(), statuses.size());
            throw new AssertionError(error);
        }
        adapter.addAll(0, ancestorsViewDatas);

        if (mainStatus != null) {
            // In case we needed to delete everything (which is way easier than deleting
            // everything except one), re-insert the remaining status here.
            statuses.add(statusIndex, mainStatus);
            StatusViewData.Concrete viewData = statuses.getPairedItem(statusIndex);

            adapter.addItem(statusIndex, viewData);
        }

        // Insert newly fetched descendants
        statuses.addAll(descendants);
        List<StatusViewData.Concrete> descendantsViewData;
        descendantsViewData = statuses.getPairedCopy()
                .subList(statuses.size() - descendants.size(), statuses.size());
        if (BuildConfig.DEBUG && descendants.size() != descendantsViewData.size()) {
            String error = String.format(Locale.getDefault(),
                    "Incorrectly got statusViewData sublist." +
                            " descendants.size == %d descendantsViewData.size == %d," +
                            " statuses.size == %d",
                    descendants.size(), descendantsViewData.size(), statuses.size());
            throw new AssertionError(error);
        }
        adapter.addAll(descendantsViewData);
        updateRevealIcon();
    }

    private void handleFavEvent(FavouriteEvent event) {
        Log.d("EVENT", "Count V: " + (event.getStatusOld() != null ? event.getStatusOld().getFavouritesCount() : 0) + " ->" + (event.getStatusNew() != null ? event.getStatusNew().getFavouritesCount() : 0));

        Pair<Integer, Status> posAndStatus = findStatusAndPos(event.getStatusId());
        if (posAndStatus == null) return;

        boolean favourite = event.getFavourite();
        posAndStatus.second.setFavourited(favourite);
        posAndStatus.second.setFavouritesCount(event.getStatusNew() != null ? event.getStatusNew().getFavouritesCount() : event.getStatusOld().getFavouritesCount());

        if (posAndStatus.second.getReblog() != null) {
            posAndStatus.second.getReblog().setFavourited(favourite);
            posAndStatus.second.getReblog().setFavouritesCount(event.getStatusNew() != null ? event.getStatusNew().getFavouritesCount() : event.getStatusOld().getFavouritesCount());
        }

        StatusViewData.Concrete viewdata = statuses.getPairedItem(posAndStatus.first);

        StatusViewData.Builder viewDataBuilder = new StatusViewData.Builder((viewdata));
        viewDataBuilder.setFavourited(favourite);
        viewDataBuilder.setFavouritesCount(event.getStatusNew() != null ? event.getStatusNew().getFavouritesCount() : event.getStatusOld().getFavouritesCount());

        StatusViewData.Concrete newViewData = viewDataBuilder.createStatusViewData();

        statuses.setPairedItem(posAndStatus.first, newViewData);
        adapter.setItem(posAndStatus.first, newViewData, true);
    }

    private void handleReblogEvent(ReblogEvent event) {
        Pair<Integer, Status> posAndStatus = findStatusAndPos(event.getStatusId());
        if (posAndStatus == null) return;

        boolean reblog = event.getReblog();
        posAndStatus.second.setReblogged(reblog);

        if (posAndStatus.second.getReblog() != null) {
            posAndStatus.second.getReblog().setReblogged(reblog);
        }
        if (event.getStatusNew() != null) {
            posAndStatus.second.setReblogsCount(event.getStatusNew().getReblogsCount());

            if (posAndStatus.second.getReblog() != null) {
                posAndStatus.second.getReblog().setReblogged(reblog);
                posAndStatus.second.getReblog().setReblogsCount(event.getStatusNew().getReblogsCount());
            }

        }
        StatusViewData.Concrete viewdata = statuses.getPairedItem(posAndStatus.first);

        StatusViewData.Builder viewDataBuilder = new StatusViewData.Builder((viewdata));
        viewDataBuilder.setReblogged(reblog);
        viewDataBuilder.setReblogsCount(posAndStatus.second.getReblogsCount());

        StatusViewData.Concrete newViewData = viewDataBuilder.createStatusViewData();

        statuses.setPairedItem(posAndStatus.first, newViewData);
        adapter.setItem(posAndStatus.first, newViewData, true);
    }

    private void handleStatusComposedEvent(StatusComposedEvent event) {
        Status eventStatus = event.getStatus();
        if (eventStatus.getInReplyToId() == null) return;

        if (eventStatus.getInReplyToId().equals(thisThreadsStatusId)) {
            insertStatus(eventStatus, statuses.size());
            addReplyCount(0, 1);
        } else {
            // If new status is a reply to some status in the thread, insert new status after it
            // We only check statuses below main status, ones on top don't belong to this thread
            for (int i = statusIndex; i < statuses.size(); i++) {
                Status status = statuses.get(i);
                if (eventStatus.getInReplyToId().equals(status.getId())) {
                    addReplyCount(i, 1);
                    insertStatus(eventStatus, i + 1);
                    break;
                }
            }
        }
    }

    private void addReplyCount(int idx, int countToAdd) {
        Status status = statuses.get(idx);
        int newRepliesCount = status.getRepliesCount() + countToAdd;
        status.setRepliesCount(newRepliesCount >= 0 ? newRepliesCount : 0);

        StatusViewData.Concrete viewdata = statuses.getPairedItem(idx);

        StatusViewData.Builder viewDataBuilder = new StatusViewData.Builder((viewdata));
        viewDataBuilder.setRepliesCount(status.getRepliesCount());

        StatusViewData.Concrete newViewData = viewDataBuilder.createStatusViewData();

        statuses.setPairedItem(idx, newViewData);
        adapter.setItem(idx, newViewData, true);
    }

    private void insertStatus(Status status, int at) {
        statuses.add(at, status);
        adapter.addItem(at, statuses.getPairedItem(at));
    }

    private void handleStatusDeletedEvent(StatusDeletedEvent event) {
        replyDeleted(event.getStatusId());
    }

    private void replyDeleted(String statusId) {
        Pair<Integer, Status> posAndStatus = findStatusAndPos(statusId);
        if (posAndStatus == null) return;

        @SuppressWarnings("ConstantConditions")
        int pos = posAndStatus.first;
        Status status = posAndStatus.second;
        if (status != null && status.getInReplyToId() != null) {
            Pair<Integer, Status> statusPosition = findStatusAndPos(status.getInReplyToId());
            if (statusPosition != null && statusPosition.first != null && statusPosition.first >= 0) {
                addReplyCount(statusPosition.first, -1);
            }
        }

        if (statuses.remove(status))
            adapter.removeItem(pos);

    }

    @Nullable
    private Pair<Integer, Status> findStatusAndPos(@NonNull String statusId) {
        for (int i = 0; i < statuses.size(); i++) {
            if (statusId.equals(statuses.get(i).getId())) {
                return new Pair<>(i, statuses.get(i));
            }
        }
        return null;
    }

    private void updateRevealIcon() {
        ViewThreadActivity activity = ((ViewThreadActivity) getActivity());
        if (activity == null) return;

        boolean hasAnyWarnings = false;
        // Statuses are updated from the main thread so nothing should change while iterating
        for (int i = 0; i < statuses.size(); i++) {
            if (!TextUtils.isEmpty(statuses.get(i).getSpoilerText())) {
                hasAnyWarnings = true;
                break;
            }
        }
        if (!hasAnyWarnings) {
            activity.setRevealButtonState(ViewThreadActivity.REVEAL_BUTTON_HIDDEN);
            return;
        }
        activity.setRevealButtonState(allExpanded() ? ViewThreadActivity.REVEAL_BUTTON_HIDE :
                ViewThreadActivity.REVEAL_BUTTON_REVEAL);
    }
}