package com.example.beechats.data.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beechats.data.models.BlockedUser;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

/**
 * Unit tests cho BlockRepository (TC175 – TC188).
 * Mock Firestore chain để test không cần device/emulator.
 */
public class BlockRepositoryTest {

    // ---------- mocks cơ bản ----------
    @Mock private FirebaseFirestore mockFirestore;

    /** Collection "blockedUsers" */
    @Mock private CollectionReference mockBlockedUsersCollection;

    /** Document blockedUsers/{userId} */
    @Mock private DocumentReference mockUserDoc;

    /** Subcollection blockedUsers/{userId}/blockedList */
    @Mock private CollectionReference mockBlockedListCollection;

    /** Document blockedUsers/{userId}/blockedList/{targetId} */
    @Mock private DocumentReference mockTargetRef;

    @Mock private BlockRepository.OnCompleteCallback mockCompleteCallback;
    @Mock private BlockRepository.OnBooleanCallback mockBooleanCallback;
    @Mock private BlockRepository.OnBlockedListCallback mockListCallback;

    private BlockRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // db.collection("blockedUsers") → mockBlockedUsersCollection
        when(mockFirestore.collection("blockedUsers")).thenReturn(mockBlockedUsersCollection);

        // mockBlockedUsersCollection.document(anyString()) → mockUserDoc
        when(mockBlockedUsersCollection.document(anyString())).thenReturn(mockUserDoc);

        // mockUserDoc.collection("blockedList") → mockBlockedListCollection
        when(mockUserDoc.collection("blockedList")).thenReturn(mockBlockedListCollection);

        // mockBlockedListCollection.document(anyString()) → mockTargetRef
        when(mockBlockedListCollection.document(anyString())).thenReturn(mockTargetRef);

        repository = new BlockRepository(mockFirestore);
    }

    // -----------------------------------------------------------------------
    // TC175: blockUser — userId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void blockUser_nullUserId_callsOnError() {
        repository.blockUser(null, "uid-002", mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID người dùng không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC176: blockUser — targetUserId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void blockUser_nullTargetId_callsOnError() {
        repository.blockUser("uid-001", null, mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID người bị chặn không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC177: blockUser — params hợp lệ → Firestore set → onSuccess
    // -----------------------------------------------------------------------

    @Test
    public void blockUser_validParams_callsFirestoreSetAndOnSuccess() {
        Task<Void> task = buildVoidSuccessTask();
        when(mockTargetRef.set(anyMap())).thenReturn(task);

        repository.blockUser("uid-001", "uid-002", mockCompleteCallback);

        verify(mockTargetRef).set(anyMap());
        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC178: blockUser — Firestore fail → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void blockUser_firestoreFails_callsOnError() {
        Exception ex = new Exception("Firestore error");
        Task<Void> task = buildVoidFailureTask(ex);
        when(mockTargetRef.set(anyMap())).thenReturn(task);

        repository.blockUser("uid-001", "uid-002", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Firestore error");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC179: unblockUser — userId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void unblockUser_nullUserId_callsOnError() {
        repository.unblockUser(null, "uid-002", mockCompleteCallback);

        verify(mockCompleteCallback).onError("ID người dùng không hợp lệ.");
        verify(mockCompleteCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC180: unblockUser — params hợp lệ → Firestore delete → onSuccess
    // -----------------------------------------------------------------------

    @Test
    public void unblockUser_validParams_callsFirestoreDeleteAndOnSuccess() {
        Task<Void> task = buildVoidSuccessTask();
        when(mockTargetRef.delete()).thenReturn(task);

        repository.unblockUser("uid-001", "uid-002", mockCompleteCallback);

        verify(mockTargetRef).delete();
        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC181: unblockUser — Firestore fail → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void unblockUser_firestoreFails_callsOnError() {
        Exception ex = new Exception("Network error");
        Task<Void> task = buildVoidFailureTask(ex);
        when(mockTargetRef.delete()).thenReturn(task);

        repository.unblockUser("uid-001", "uid-002", mockCompleteCallback);

        verify(mockCompleteCallback).onError("Network error");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC182: isBlocked — userId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void isBlocked_nullUserId_callsOnError() {
        repository.isBlocked(null, "uid-002", mockBooleanCallback);

        verify(mockBooleanCallback).onError("ID người dùng không hợp lệ.");
        verify(mockBooleanCallback, never()).onResult(true);
        verify(mockBooleanCallback, never()).onResult(false);
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC183: isBlocked — document tồn tại → onResult(true)
    // -----------------------------------------------------------------------

    @Test
    public void isBlocked_documentExists_callsOnResultTrue() {
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(true);

        Task<DocumentSnapshot> task = buildDocSuccessTask(mockSnapshot);
        when(mockTargetRef.get()).thenReturn(task);

        repository.isBlocked("uid-001", "uid-002", mockBooleanCallback);

        verify(mockBooleanCallback).onResult(true);
        verify(mockBooleanCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC184: isBlocked — document không tồn tại → onResult(false)
    // -----------------------------------------------------------------------

    @Test
    public void isBlocked_documentNotExists_callsOnResultFalse() {
        DocumentSnapshot mockSnapshot = mock(DocumentSnapshot.class);
        when(mockSnapshot.exists()).thenReturn(false);

        Task<DocumentSnapshot> task = buildDocSuccessTask(mockSnapshot);
        when(mockTargetRef.get()).thenReturn(task);

        repository.isBlocked("uid-001", "uid-002", mockBooleanCallback);

        verify(mockBooleanCallback).onResult(false);
        verify(mockBooleanCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC185: isBlocked — Firestore fail → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void isBlocked_firestoreFails_callsOnError() {
        Exception ex = new Exception("Permission denied");
        Task<DocumentSnapshot> task = buildDocFailureTask(ex);
        when(mockTargetRef.get()).thenReturn(task);

        repository.isBlocked("uid-001", "uid-002", mockBooleanCallback);

        verify(mockBooleanCallback).onError("Permission denied");
        verify(mockBooleanCallback, never()).onResult(true);
        verify(mockBooleanCallback, never()).onResult(false);
    }

    // -----------------------------------------------------------------------
    // TC186: getBlockedList — userId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void getBlockedList_nullUserId_callsOnError() {
        repository.getBlockedList(null, mockListCallback);

        verify(mockListCallback).onError("ID người dùng không hợp lệ.");
        verify(mockListCallback, never()).onSuccess(any());
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC187: getBlockedList — Firestore success → onSuccess(list)
    // -----------------------------------------------------------------------

    @Test
    public void getBlockedList_firestoreSuccess_callsOnSuccessWithList() {
        BlockedUser blockedUser = new BlockedUser();
        blockedUser.setBlockedUserId("uid-002");

        DocumentSnapshot mockDoc = mock(DocumentSnapshot.class);
        when(mockDoc.toObject(BlockedUser.class)).thenReturn(blockedUser);

        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.singletonList(mockDoc));

        Task<QuerySnapshot> task = buildQuerySuccessTask(mockQuerySnapshot);
        when(mockBlockedListCollection.get()).thenReturn(task);

        repository.getBlockedList("uid-001", mockListCallback);

        verify(mockListCallback).onSuccess(any(List.class));
        verify(mockListCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC188: getBlockedList — Firestore fail → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void getBlockedList_firestoreFails_callsOnError() {
        Exception ex = new Exception("Read error");
        Task<QuerySnapshot> task = buildQueryFailureTask(ex);
        when(mockBlockedListCollection.get()).thenReturn(task);

        repository.getBlockedList("uid-001", mockListCallback);

        verify(mockListCallback).onError("Read error");
        verify(mockListCallback, never()).onSuccess(any());
    }

    // -----------------------------------------------------------------------
    // Helper: Task<Void> thành công
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Helper: Task<Void> thất bại
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<Void> buildVoidFailureTask(Exception exception) {
        Task<Void> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // Helper: Task<DocumentSnapshot> thành công
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<DocumentSnapshot> buildDocSuccessTask(DocumentSnapshot snapshot) {
        Task<DocumentSnapshot> mockTask = mock(Task.class);
        doAnswer(inv -> {
            ((OnSuccessListener<DocumentSnapshot>) inv.getArgument(0)).onSuccess(snapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTask);
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // Helper: Task<DocumentSnapshot> thất bại
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<DocumentSnapshot> buildDocFailureTask(Exception exception) {
        Task<DocumentSnapshot> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // Helper: Task<QuerySnapshot> thành công
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Helper: Task<QuerySnapshot> thất bại
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<QuerySnapshot> buildQueryFailureTask(Exception exception) {
        Task<QuerySnapshot> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }
}
