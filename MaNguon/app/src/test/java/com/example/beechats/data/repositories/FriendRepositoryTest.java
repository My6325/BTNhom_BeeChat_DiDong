package com.example.beechats.data.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beechats.data.models.Friend;
import com.example.beechats.data.models.FriendRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests cho FriendRepository (TC143 – TC165).
 * Mock Firestore chain để test không cần device/emulator.
 */
public class FriendRepositoryTest {

    // ---------- mocks cơ bản ----------
    @Mock private FirebaseFirestore mockFirestore;

    /** Collection "friendRequests" */
    @Mock private CollectionReference mockFriendRequestsCollection;

    /** Document trong friendRequests (auto-ID) */
    @Mock private DocumentReference mockRequestRef;

    /** Collection "friends" */
    @Mock private CollectionReference mockFriendsCollection;

    /** Document friends/{uid} */
    @Mock private DocumentReference mockFriendsDoc;

    /** Subcollection friends/{uid}/friendList */
    @Mock private CollectionReference mockFriendListCollection;

    /** Document trong friendList/{friendUid} */
    @Mock private DocumentReference mockFriendRef;

    /** WriteBatch */
    @Mock private WriteBatch mockBatch;

    /** Query (để stub whereEqualTo chain cho getPendingRequests) */
    @Mock private Query mockQuery;

    /** QuerySnapshot (kết quả get() trả về) */
    @Mock private QuerySnapshot mockQuerySnapshot;

    // ---------- callbacks ----------
    @Mock private FriendRepository.OnFriendRequestCallback mockRequestCallback;
    @Mock private FriendRepository.OnCompleteCallback mockCompleteCallback;
    @Mock private FriendRepository.OnFriendListCallback mockFriendListCallback;
    @Mock private FriendRepository.OnRequestListCallback mockRequestListCallback;

    private FriendRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // --- stub collection("friendRequests") ---
        when(mockFirestore.collection("friendRequests")).thenReturn(mockFriendRequestsCollection);
        // auto-ID document
        when(mockFriendRequestsCollection.document()).thenReturn(mockRequestRef);
        when(mockRequestRef.getId()).thenReturn("reqId001");
        // named document (for decline / accept)
        when(mockFriendRequestsCollection.document(anyString())).thenReturn(mockRequestRef);

        // --- stub collection("friends") ---
        when(mockFirestore.collection("friends")).thenReturn(mockFriendsCollection);
        when(mockFriendsCollection.document(anyString())).thenReturn(mockFriendsDoc);
        when(mockFriendsDoc.collection(anyString())).thenReturn(mockFriendListCollection);
        when(mockFriendListCollection.document(anyString())).thenReturn(mockFriendRef);

        // --- stub WriteBatch ---
        when(mockFirestore.batch()).thenReturn(mockBatch);
        when(mockBatch.update(any(DocumentReference.class), anyMap())).thenReturn(mockBatch);
        when(mockBatch.set(any(DocumentReference.class), anyMap())).thenReturn(mockBatch);
        when(mockBatch.delete(any(DocumentReference.class))).thenReturn(mockBatch);

        // --- stub whereEqualTo chain cho getPendingRequests ---
        when(mockFriendRequestsCollection.whereEqualTo(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo(anyString(), any())).thenReturn(mockQuery);

        repository = new FriendRepository(mockFirestore);
    }

    // -----------------------------------------------------------------------
    // TC143: sendFriendRequest — fromUid null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void sendFriendRequest_fromUidNull_callsOnError() {
        repository.sendFriendRequest(null, "uid_B", mockRequestCallback);

        verify(mockRequestCallback).onError(anyString());
        verify(mockRequestCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC144: sendFriendRequest — toUid blank → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void sendFriendRequest_toUidBlank_callsOnError() {
        repository.sendFriendRequest("uid_A", "   ", mockRequestCallback);

        verify(mockRequestCallback).onError(anyString());
        verify(mockRequestCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC145: sendFriendRequest — gửi lời mời cho chính mình → onError
    // -----------------------------------------------------------------------

    @Test
    public void sendFriendRequest_sameUid_callsOnError() {
        repository.sendFriendRequest("uid_A", "uid_A", mockRequestCallback);

        verify(mockRequestCallback).onError(anyString());
        verify(mockRequestCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC146: sendFriendRequest — happy path → requestRef.set() + onSuccess(requestId)
    // -----------------------------------------------------------------------

    @Test
    public void sendFriendRequest_validParams_callsSetAndOnSuccess() {
        Task<Void> setTask = buildVoidSuccessTask();
        when(mockRequestRef.set(anyMap())).thenReturn(setTask);

        repository.sendFriendRequest("uid_A", "uid_B", mockRequestCallback);

        verify(mockRequestRef).set(anyMap());
        verify(mockRequestCallback).onSuccess("reqId001");
        verify(mockRequestCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC147: sendFriendRequest — Firestore set fail → onError
    // -----------------------------------------------------------------------

    @Test
    public void sendFriendRequest_firestoreFails_callsOnError() {
        Task<Void> failTask = buildVoidFailureTask(new Exception("Network error"));
        when(mockRequestRef.set(anyMap())).thenReturn(failTask);

        repository.sendFriendRequest("uid_A", "uid_B", mockRequestCallback);

        verify(mockRequestCallback).onError("Network error");
        verify(mockRequestCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // TC148: acceptFriendRequest — requestId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void acceptFriendRequest_requestIdNull_callsOnError() {
        repository.acceptFriendRequest(null, "uid_A", "uid_B", mockCompleteCallback);

        verify(mockCompleteCallback).onError(anyString());
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC149: acceptFriendRequest — fromUid blank → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void acceptFriendRequest_fromUidBlank_callsOnError() {
        repository.acceptFriendRequest("req1", "  ", "uid_B", mockCompleteCallback);

        verify(mockCompleteCallback).onError(anyString());
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC150: acceptFriendRequest — happy path → batch.commit() + onSuccess
    //        Xác nhận tạo friends cả 2 phía (batch.set x2)
    // -----------------------------------------------------------------------

    @Test
    public void acceptFriendRequest_validParams_commitsBatchAndOnSuccess() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.acceptFriendRequest("req1", "uid_A", "uid_B", mockCompleteCallback);

        verify(mockBatch).commit();
        // Cập nhật friendRequests status
        verify(mockBatch).update(eq(mockRequestRef), anyMap());
        // Tạo friends/uid_A/friendList/uid_B và friends/uid_B/friendList/uid_A
        verify(mockBatch, org.mockito.Mockito.times(2)).set(eq(mockFriendRef), anyMap());
        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC151: acceptFriendRequest — Firestore commit fail → onError
    // -----------------------------------------------------------------------

    @Test
    public void acceptFriendRequest_batchCommitFails_callsOnError() {
        Task<Void> failTask = buildVoidFailureTask(new Exception("Commit failed"));
        when(mockBatch.commit()).thenReturn(failTask);

        repository.acceptFriendRequest("req1", "uid_A", "uid_B", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Commit failed");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC152: declineFriendRequest — requestId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void declineFriendRequest_requestIdNull_callsOnError() {
        repository.declineFriendRequest(null, mockCompleteCallback);

        verify(mockCompleteCallback).onError(anyString());
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC153: declineFriendRequest — requestId blank → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void declineFriendRequest_requestIdBlank_callsOnError() {
        repository.declineFriendRequest("   ", mockCompleteCallback);

        verify(mockCompleteCallback).onError(anyString());
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC154: declineFriendRequest — happy path → requestRef.update() + onSuccess
    // -----------------------------------------------------------------------

    @Test
    public void declineFriendRequest_validId_updatesStatusAndOnSuccess() {
        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockRequestRef.update(anyMap())).thenReturn(updateTask);

        repository.declineFriendRequest("req1", mockCompleteCallback);

        verify(mockRequestRef).update(anyMap());
        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC155: declineFriendRequest — Firestore update fail → onError
    // -----------------------------------------------------------------------

    @Test
    public void declineFriendRequest_firestoreFails_callsOnError() {
        Task<Void> failTask = buildVoidFailureTask(new Exception("Permission denied"));
        when(mockRequestRef.update(anyMap())).thenReturn(failTask);

        repository.declineFriendRequest("req1", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Permission denied");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC156: unfriend — uidA null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void unfriend_uidANull_callsOnError() {
        repository.unfriend(null, "uid_B", mockCompleteCallback);

        verify(mockCompleteCallback).onError(anyString());
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC157: unfriend — uidB blank → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void unfriend_uidBBlank_callsOnError() {
        repository.unfriend("uid_A", "", mockCompleteCallback);

        verify(mockCompleteCallback).onError(anyString());
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC158: unfriend — happy path → batch.delete x2 + commit + onSuccess
    // -----------------------------------------------------------------------

    @Test
    public void unfriend_validUids_deletesFromBothSidesAndOnSuccess() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.unfriend("uid_A", "uid_B", mockCompleteCallback);

        verify(mockBatch).commit();
        // Xóa friends/uid_A/friendList/uid_B và friends/uid_B/friendList/uid_A
        verify(mockBatch, org.mockito.Mockito.times(2)).delete(eq(mockFriendRef));
        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC159: unfriend — Firestore commit fail → onError
    // -----------------------------------------------------------------------

    @Test
    public void unfriend_batchCommitFails_callsOnError() {
        Task<Void> failTask = buildVoidFailureTask(new Exception("Offline"));
        when(mockBatch.commit()).thenReturn(failTask);

        repository.unfriend("uid_A", "uid_B", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Offline");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC160: getPendingRequests — uid null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void getPendingRequests_uidNull_callsOnError() {
        repository.getPendingRequests(null, mockRequestListCallback);

        verify(mockRequestListCallback).onError(anyString());
        verify(mockRequestListCallback, never()).onSuccess(any());
    }

    // -----------------------------------------------------------------------
    // TC161: getPendingRequests — uid blank → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void getPendingRequests_uidBlank_callsOnError() {
        repository.getPendingRequests("  ", mockRequestListCallback);

        verify(mockRequestListCallback).onError(anyString());
        verify(mockRequestListCallback, never()).onSuccess(any());
    }

    // -----------------------------------------------------------------------
    // TC162: getPendingRequests — trả về danh sách rỗng khi không có request
    // -----------------------------------------------------------------------

    @Test
    public void getPendingRequests_noResults_callsOnSuccessWithEmptyList() {
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.emptyList());
        Task<QuerySnapshot> queryTask = buildQuerySuccessTask(mockQuerySnapshot);
        when(mockQuery.get()).thenReturn(queryTask);

        repository.getPendingRequests("uid_B", mockRequestListCallback);

        verify(mockRequestListCallback).onSuccess(org.mockito.ArgumentMatchers.argThat(list -> list != null && list.isEmpty()));
        verify(mockRequestListCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC163: getPendingRequests — Firestore get fail → onError
    // -----------------------------------------------------------------------

    @Test
    public void getPendingRequests_firestoreFails_callsOnError() {
        Task<QuerySnapshot> failTask = buildQueryFailureTask(new Exception("Server error"));
        when(mockQuery.get()).thenReturn(failTask);

        repository.getPendingRequests("uid_B", mockRequestListCallback);

        verify(mockRequestListCallback).onError("Server error");
        verify(mockRequestListCallback, never()).onSuccess(any());
    }

    // -----------------------------------------------------------------------
    // TC164: getFriendList — uid null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void getFriendList_uidNull_callsOnError() {
        repository.getFriendList(null, mockFriendListCallback);

        verify(mockFriendListCallback).onError(anyString());
        verify(mockFriendListCallback, never()).onSuccess(any());
    }

    // -----------------------------------------------------------------------
    // TC165: getFriendList — uid blank → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void getFriendList_uidBlank_callsOnError() {
        repository.getFriendList("", mockFriendListCallback);

        verify(mockFriendListCallback).onError(anyString());
        verify(mockFriendListCallback, never()).onSuccess(any());
    }

    // -----------------------------------------------------------------------
    // TC166: getFriendList — trả về danh sách rỗng
    // -----------------------------------------------------------------------

    @Test
    public void getFriendList_noFriends_callsOnSuccessWithEmptyList() {
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.emptyList());
        Task<QuerySnapshot> queryTask = buildQuerySuccessTask(mockQuerySnapshot);
        when(mockFriendListCollection.get()).thenReturn(queryTask);

        repository.getFriendList("uid_A", mockFriendListCallback);

        verify(mockFriendListCallback).onSuccess(org.mockito.ArgumentMatchers.argThat(list -> list != null && list.isEmpty()));
        verify(mockFriendListCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC167: getFriendList — Firestore get fail → onError
    // -----------------------------------------------------------------------

    @Test
    public void getFriendList_firestoreFails_callsOnError() {
        Task<QuerySnapshot> failTask = buildQueryFailureTask(new Exception("Permission denied"));
        when(mockFriendListCollection.get()).thenReturn(failTask);

        repository.getFriendList("uid_A", mockFriendListCallback);

        verify(mockFriendListCallback).onError("Permission denied");
        verify(mockFriendListCallback, never()).onSuccess(any());
    }

    // =======================================================================
    // Helper methods
    // =======================================================================

    /** Tạo mock Task<Void> thành công (gọi onSuccess listener ngay). */
    @SuppressWarnings("unchecked")
    private Task<Void> buildVoidSuccessTask() {
        Task<Void> mockTask = mock(Task.class);
        doAnswer(inv -> {
            ((OnSuccessListener<Void>) inv.getArgument(0)).onSuccess(null);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTask);
        return mockTask;
    }

    /** Tạo mock Task<Void> thất bại (gọi onFailure listener ngay). */
    @SuppressWarnings("unchecked")
    private Task<Void> buildVoidFailureTask(Exception e) {
        Task<Void> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(e);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }

    /** Tạo mock Task<QuerySnapshot> thành công. */
    @SuppressWarnings("unchecked")
    private Task<QuerySnapshot> buildQuerySuccessTask(QuerySnapshot snapshot) {
        Task<QuerySnapshot> mockTask = mock(Task.class);
        doAnswer(inv -> {
            ((OnSuccessListener<QuerySnapshot>) inv.getArgument(0)).onSuccess(snapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTask);
        return mockTask;
    }

    /** Tạo mock Task<QuerySnapshot> thất bại. */
    @SuppressWarnings("unchecked")
    private Task<QuerySnapshot> buildQueryFailureTask(Exception e) {
        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(e);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }
}
