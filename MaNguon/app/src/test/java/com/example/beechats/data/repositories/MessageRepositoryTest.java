package com.example.beechats.data.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import com.google.firebase.firestore.WriteBatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

/**
 * Unit tests cho MessageRepository.
 * Mock Firestore + WriteBatch chain để test không cần device/emulator.
 */
public class MessageRepositoryTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockCollection;

    /** conversations/{id} document — dùng cả để lấy subcollection messages và làm convRef */
    @Mock
    private DocumentReference mockConvDocRef;

    @Mock
    private CollectionReference mockMsgSubCollection;

    /** message document mới (auto-ID) */
    @Mock
    private DocumentReference mockMsgRef;

    @Mock
    private WriteBatch mockBatch;

    @Mock
    private MessageRepository.OnSendMessageCallback mockCallback;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapCaptor;

    private MessageRepository repository;

    private static final String CONV_ID = "aaaa_bbbb";
    private static final String SENDER_ID = "aaaa";
    private static final String SENDER_NAME = "User A Test";
    private static final String MESSAGE_TEXT = "Xin chào";
    private static final String TEST_MESSAGE_ID = "testMessageId001";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Stub Firestore chain:
        // db.collection("conversations").document(convId) → mockConvDocRef
        // mockConvDocRef.collection("messages").document() → mockMsgRef
        when(mockFirestore.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockConvDocRef);
        when(mockConvDocRef.collection(anyString())).thenReturn(mockMsgSubCollection);
        when(mockMsgSubCollection.document()).thenReturn(mockMsgRef); // no-arg auto-ID

        when(mockMsgRef.getId()).thenReturn(TEST_MESSAGE_ID);
        when(mockFirestore.batch()).thenReturn(mockBatch);
        when(mockBatch.set(any(DocumentReference.class), anyMap())).thenReturn(mockBatch);
        when(mockBatch.update(any(DocumentReference.class), anyMap())).thenReturn(mockBatch);

        repository = new MessageRepository(mockFirestore);
    }

    // -----------------------------------------------------------------------
    // TC31: Happy path — tham số hợp lệ → batch.commit() gọi → onSuccess(messageId)
    // -----------------------------------------------------------------------

    @Test
    public void sendMessage_validParams_callsCommitAndOnSuccess() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        verify(mockBatch).commit();
        verify(mockCallback).onSuccess(TEST_MESSAGE_ID);
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC32: lastMessage.text trong batch.update() khớp với text đã gửi
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Test
    public void sendMessage_validParams_lastMessageTextMatchesSentText() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        // Bắt map truyền vào batch.update(convRef, convUpdate)
        verify(mockBatch).update(any(DocumentReference.class), mapCaptor.capture());
        Map<String, Object> convUpdate = mapCaptor.getValue();
        Map<String, Object> lastMessage = (Map<String, Object>) convUpdate.get("lastMessage");

        assertNotNull(lastMessage);
        assertEquals(MESSAGE_TEXT, lastMessage.get("text"));
        assertEquals(SENDER_ID, lastMessage.get("senderId"));
    }

    // -----------------------------------------------------------------------
    // TC33: convUpdate trong batch.update() chứa trường updatedAt
    // -----------------------------------------------------------------------

    @Test
    public void sendMessage_validParams_convUpdateContainsUpdatedAt() {
        Task<Void> commitTask = buildVoidSuccessTask();
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        verify(mockBatch).update(any(DocumentReference.class), mapCaptor.capture());
        Map<String, Object> convUpdate = mapCaptor.getValue();

        assertTrue("convUpdate phải chứa trường updatedAt", convUpdate.containsKey("updatedAt"));
    }

    // -----------------------------------------------------------------------
    // TC34: text rỗng/null → onError ngay, Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void sendMessage_emptyText_callsOnErrorWithoutFirestore() {
        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, "", mockCallback);

        verify(mockCallback).onError("Nội dung tin nhắn không được để trống.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    @Test
    public void sendMessage_nullText_callsOnErrorWithoutFirestore() {
        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, null, mockCallback);

        verify(mockCallback).onError("Nội dung tin nhắn không được để trống.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC35: conversationId null/rỗng → onError ngay, Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void sendMessage_nullConversationId_callsOnErrorWithoutFirestore() {
        repository.sendMessage(null, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        verify(mockCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    @Test
    public void sendMessage_emptyConversationId_callsOnErrorWithoutFirestore() {
        repository.sendMessage("", SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

        verify(mockCallback).onError("ID hội thoại không hợp lệ.");
        verify(mockCallback, never()).onSuccess(anyString());
        verify(mockFirestore, never()).batch();
    }

    // -----------------------------------------------------------------------
    // TC36: batch.commit() fail → onError(message), onSuccess() không gọi
    // -----------------------------------------------------------------------

    @Test
    public void sendMessage_commitFails_callsOnErrorWithMessage() {
        Exception exception = new Exception("Firestore network error");
        Task<Void> commitTask = buildVoidFailureTask(exception);
        when(mockBatch.commit()).thenReturn(commitTask);

        repository.sendMessage(CONV_ID, SENDER_ID, SENDER_NAME, MESSAGE_TEXT, mockCallback);

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
