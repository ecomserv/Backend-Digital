package yea.ecomservapi.modules.products.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yea.ecomservapi.modules.products.domain.Product;
import yea.ecomservapi.modules.products.dto.ProductDTO;
import yea.ecomservapi.modules.products.repository.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductDTO> searchProducts(String search) {
        List<Product> products;
        if (search == null || search.isBlank()) {
            products = productRepository.findTop20ByOrderByUpdatedAtDesc();
        } else {
            String term = search.trim();
            products = productRepository.findByCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(term, term);
        }
        return products.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ProductDTO> findById(Long id) {
        return productRepository.findById(id).map(this::toDTO);
    }

    public Optional<ProductDTO> findByCode(String code) {
        return productRepository.findByCode(code).map(this::toDTO);
    }

    public ProductDTO createProduct(ProductDTO dto) {
        Product product = toEntity(dto);
        Product saved = productRepository.save(product);
        return toDTO(saved);
    }

    public Optional<ProductDTO> updateProduct(Long id, ProductDTO dto) {
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setCode(dto.getCode());
                    existing.setDescription(dto.getDescription());
                    existing.setUnitMeasure(dto.getUnitMeasure());
                    existing.setReferencePrice(dto.getReferencePrice());
                    return toDTO(productRepository.save(existing));
                });
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .code(product.getCode())
                .description(product.getDescription())
                .unitMeasure(product.getUnitMeasure())
                .referencePrice(product.getReferencePrice())
                .build();
    }

    private Product toEntity(ProductDTO dto) {
        return Product.builder()
                .code(dto.getCode())
                .description(dto.getDescription())
                .unitMeasure(dto.getUnitMeasure() != null ? dto.getUnitMeasure() : "UND")
                .referencePrice(dto.getReferencePrice())
                .build();
    }
}
