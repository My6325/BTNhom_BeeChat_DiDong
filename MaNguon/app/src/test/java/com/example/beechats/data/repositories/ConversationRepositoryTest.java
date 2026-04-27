package com.example.beechats.data.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests cho ConversationRepository.
 * Mock Firestore chain để test không cần device/emulator.
 */
public class ConversationRepositoryTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockCollection;

    @Mock
    private DocumentReference mockDocument;

    @Mock
    private ConversationRepository.OnConversationCallback mockCallback;

    private ConversationRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Stub Firestore chain: db.collection(...).document(...) → mockDocument
        when(mockFirestore.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocument);
        repository = new ConversationRepository(mockFirestore);
    }

    // -----------------------------------------------------------------------
    // TC26: Happy path — uid hợp lệ → set() được gọi → onSuccess(conversationId)
    // -----------------------------------------------------------------------

    @Test
    public void createOrGetPrivateConversation_validUids_callsSetAndOnSuccess() {
        Task<Void> setTask = buildVoidSuccessTask();
        when(mockDocument.set(anyMap(), any(SetOptions.class))).thenReturn(setTask);

        repository.createOrGetPrivateConversation("aaaa", "bbbb", mockCallback);

        verify(mockDocument).set(anyMap(), any(SetOptions.class));
        verify(mockCallback).onSuccess("aaaa_bbbb");
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC27: Deterministic ID — đảo thứ tự uid vẫn sinh cùng conversationId
    // -----------------------------------------------------------------------

    @Test
    public void createOrGetPrivateConversation_reversedUids_sameConversationId() {
        Task<Void> setTask = buildVoidSuccessTask();
        when(mockDocument.set(anyMap(), any(SetOptions.class))).thenReturn(setTask);

        // Đảo ngược thứ tự: bbbb trước aaaa
        repository.createOrGetPrivateConversation("bbbb", "aaaa", mockCallback);

        // conversationId vẫn phải là "aaaa_bbbb" (sorted)
        verify(mockCallback).onSuccess("aaaa_bbbb");
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC28: uid1 null hoặc rỗng → onError ngay, Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void createOrGetPrivateConversation_nullUid1_callsOnErrorWithoutFirestore() {
        repository.createOrGetPrivateConversation(null, "bbbb", mockCallback);

        verify(mockCallback).onError("ID người dùng không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockDocument, never()).set(anyMap(), any(SetOptions.class));
    }

    @Test
    public void createOrGetPrivateConversation_emptyUid1_callsOnErrorWithoutFirestore() {
        repository.createOrGetPrivateConversation("", "bbbb", mockCallback);

        verify(mockCallback).onError("ID người dùng không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockDocument, never()).set(anyMap(), any(SetOptions.class));
    }

    // -----------------------------------------------------------------------
    // TC29: uid2 null hoặc rỗng → onError ngay, Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void createOrGetPrivateConversation_nullUid2_callsOnErrorWithoutFirestore() {
        repository.createOrGetPrivateConversation("aaaa", null, mockCallback);

        verify(mockCallback).onError("ID người dùng không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockDocument, never()).set(anyMap(), any(SetOptions.class));
    }

    // -----------------------------------------------------------------------
    // TC30: Firestore set() fail → onError(message), onSuccess() không gọi
    // -----------------------------------------------------------------------

    @Test
    public void createOrGetPrivateConversation_firestoreFails_callsOnErrorWithMessage() {
        Exception exception = new Exception("Firestore network error");
        Task<Void> setTask = buildVoidFailureTask(exception);
        when(mockDocument.set(anyMap(), any(SetOptions.class))).thenReturn(setTask);

        repository.createOrGetPrivateConversation("aaaa", "bbbb", mockCallback);

        verify(mockCallback).onError("Firestore network error");
        verify(mockCallback, never()).onSuccess(anyString());
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task<Void> thành công
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
    // Helper: tạo mock Task<Void> thất bại
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
}
