package com.example.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.model.Category;
import com.example.model.CategoryProduct;
import com.example.model.Product;
import com.example.repository.CategoryProductRepository;
import com.example.repository.ProductRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Predicate;

import com.example.entity.ProductWithCategoryName;
import com.example.form.ProductForm;
import com.example.form.ProductSearchForm;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryProductRepository categoryProductRepository;

	public List<Product> findAll() {
		return productRepository.findAll();
	}

	public Optional<Product> findOne(Long id) {
		return productRepository.findById(id);
	}

	@Transactional(readOnly = false)
	public Product save(Product entity) {
		return productRepository.save(entity);
	}

	@Transactional(readOnly = false)
	public void delete(Product entity) {
		productRepository.delete(entity);
	}

	// 指定された検索条件に一致するエンティティを検索する
	public List<ProductWithCategoryName> search(Long shopId, ProductSearchForm form) {
		final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		final CriteriaQuery<ProductWithCategoryName> query = builder.createQuery(ProductWithCategoryName.class);
		final Root<Product> root = query.from(Product.class);

		Join<Product, CategoryProduct> categoryProductJoin = root.joinList("categoryProducts", JoinType.LEFT);
		Join<CategoryProduct, Category> categoryJoin = categoryProductJoin.join("category", JoinType.LEFT);

		query.multiselect(
				root.get("id"),
				root.get("code"),
				root.get("name"),
				root.get("weight"),
				root.get("height"),
				root.get("price"),
				builder.coalesce(
						builder.function("GROUP_CONCAT", String.class, categoryJoin.get("name"), builder.literal(", ")),
						"Unknown Category").alias("categoryName"))
				.where(builder.equal(root.get("shopId"), shopId))
				.groupBy(root.get("id"));

		// formの値を元に検索条件を設定する
		Predicate predicate = builder.conjunction(); // 初期化

		if (!StringUtils.isEmpty(form.getName())) {
			predicate = builder.and(predicate, builder.like(root.get("name"), "%" + form.getName() + "%"));
		}

		if (!StringUtils.isEmpty(form.getCode())) {
			predicate = builder.and(predicate, builder.like(root.get("code"), "%" + form.getCode() + "%"));
		}

		if (form.getCategories() != null && form.getCategories().size() > 0) {
			for (Long categoryId : form.getCategories()) {
				Join<Product, CategoryProduct> categoryProductJoin2 = root.joinList("categoryProducts", JoinType.LEFT);
				Join<CategoryProduct, Category> categoryJoin2 = categoryProductJoin2.join("category", JoinType.LEFT);
				predicate = builder.and(predicate, builder.equal(categoryJoin2.get("id"), categoryId));
			}
			predicate = builder.and(predicate, categoryJoin.get("id").in(form.getCategories()));
		}

		// weight で範囲検索
		if (form.getWeight1() != null && form.getWeight2() != null) {
			predicate = builder.and(predicate,
					builder.between(root.get("weight"), form.getWeight1(), form.getWeight2()));
		} else if (form.getWeight1() != null) {
			predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("weight"), form.getWeight1()));
		} else if (form.getWeight2() != null) {
			predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("weight"), form.getWeight2()));
		}

		// height で範囲検索
		if (form.getHeight1() != null || form.getHeight2() != null) {
			if (form.getHeight1() != null && form.getHeight2() != null) {
				predicate = builder.and(predicate,
						builder.between(root.get("height"), form.getHeight1(), form.getHeight2()));
			} else if (form.getHeight1() != null) {
				predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("height"), form.getHeight1()));
			} else if (form.getHeight2() != null) {
				predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("height"), form.getHeight2()));
			}
		}

		// price で範囲検索
		if (form.getPrice1() != null && form.getPrice2() != null) {
			predicate = builder.and(predicate, builder.between(root.get("price"), form.getPrice1(), form.getPrice2()));
		} else if (form.getPrice1() != null) {
			predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("price"), form.getPrice1()));
		} else if (form.getPrice2() != null) {
			predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("price"), form.getPrice2()));
		}

		query.where(predicate); // 最終的な検索条件を設定

		return entityManager.createQuery(query).getResultList();
	}

	/**
	 * ProductFormの内容を元に商品情報を保存する
	 * 
	 * @param entity
	 * @return
	 */
	@Transactional(readOnly = false)
	public Product save(ProductForm entity) {
		// 紐づくカテゴリを事前に取得
		List<CategoryProduct> categoryProducts = entity.getId() != null
				? categoryProductRepository.findByProductId(entity.getId())
				: new ArrayList<>();

		Product product = new Product(entity);
		productRepository.save(product);

		// 未処理のカテゴリーIDのリスト
		List<Long> categoryIds = entity.getCategoryIds();
		// カテゴリの紐付け解除
		for (CategoryProduct categoryProduct : categoryProducts) {
			// 紐づくカテゴリーIDが更新後のカテゴリーIDに含まれていない場合は削除
			if (!categoryIds.contains(categoryProduct.getCategoryId())) {
				categoryProductRepository.delete(categoryProduct);
			}
			// 処理が終わったものをリストから削除
			categoryIds.remove(categoryProduct.getCategoryId());
		}
		// カテゴリの紐付け登録
		for (Long categoryId : categoryIds) {
			categoryProductRepository.save(new CategoryProduct(categoryId, product.getId()));
		}

		return product;
	}
}
