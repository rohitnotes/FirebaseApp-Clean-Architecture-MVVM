package ro.alexmamo.firebaseapp.main.products;

import androidx.annotation.NonNull;
import androidx.paging.PageKeyedDataSource;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import ro.alexmamo.firebaseapp.data.Product;

import static com.google.firebase.firestore.Query.Direction.ASCENDING;
import static ro.alexmamo.firebaseapp.utils.Constants.ESCAPE_CHARACTER;
import static ro.alexmamo.firebaseapp.utils.Constants.PRODUCTS_PER_PAGE;
import static ro.alexmamo.firebaseapp.utils.Constants.PRODUCT_NAME_PROPERTY;
import static ro.alexmamo.firebaseapp.utils.HelperClass.logErrorMessage;

@SuppressWarnings("ConstantConditions")
public class ProductsDataSource extends PageKeyedDataSource<Integer, Product> {
    private String searchText;
    private Query initialQuery;
    private DocumentSnapshot lastVisible;
    private boolean lastPageReached;
    private int pageNumber = 1;

    ProductsDataSource(String searchText, CollectionReference productsRef) {
        this.searchText = searchText;
        initialQuery = productsRef.orderBy(PRODUCT_NAME_PROPERTY, ASCENDING).limit(PRODUCTS_PER_PAGE);
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params, @NonNull final LoadInitialCallback<Integer, Product> callback) {
        if (searchText != null) {
            initialQuery = initialQuery.startAt(searchText).endAt(searchText + ESCAPE_CHARACTER);
        }
        initialQuery.get().addOnCompleteListener(initialTask -> {
            List<Product> initialProductList = new ArrayList<>();
            if (initialTask.isSuccessful()) {
                QuerySnapshot initialQuerySnapshot = initialTask.getResult();
                for (QueryDocumentSnapshot document : initialQuerySnapshot) {
                    Product product = document.toObject(Product.class);
                    initialProductList.add(product);
                }
                callback.onResult(initialProductList, null, pageNumber);
                int initialQuerySnapshotSize = initialQuerySnapshot.size() - 1;
                if (initialQuerySnapshotSize != -1) {
                    lastVisible = initialQuerySnapshot.getDocuments().get(initialQuerySnapshotSize);
                }
            } else {
                logErrorMessage(initialTask.getException().getMessage());
            }
        });
    }

    @Override
    public void loadBefore(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Product> callback) {}

    @Override
    public void loadAfter(@NonNull final LoadParams<Integer> params, @NonNull final LoadCallback<Integer, Product> callback) {
        Query nextQuery = initialQuery.startAfter(lastVisible);
        nextQuery.get().addOnCompleteListener(nextTask -> {
            List<Product> nextProductList = new ArrayList<>();
            if (nextTask.isSuccessful()) {
                QuerySnapshot nextQuerySnapshot = nextTask.getResult();
                if(!lastPageReached) {
                    for(QueryDocumentSnapshot document : nextQuerySnapshot){
                        Product product = document.toObject(Product.class);
                        nextProductList.add(product);
                    }
                    callback.onResult(nextProductList, pageNumber);
                    pageNumber++;

                    int nextQuerySnapshotSize = nextQuerySnapshot.size() - 1;
                    if (nextProductList.size() < PRODUCTS_PER_PAGE) {
                        lastPageReached = true;
                    } else {
                        lastVisible = nextQuerySnapshot.getDocuments().get(nextQuerySnapshotSize);
                    }
                }
            } else {
                logErrorMessage(nextTask.getException().getMessage());
            }
        });
    }
}