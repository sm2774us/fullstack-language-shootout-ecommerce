// Package internal holds catalog business logic hidden from other modules.
package internal

import (
	"strings"

	"github.com/example/ecommerce-shootout/backend-golang/modules/catalog/infrastructure"
)

type CatalogService struct {
	products map[string]infrastructure.Product
}

func NewCatalogService() *CatalogService {
	m := map[string]infrastructure.Product{}
	for _, p := range infrastructure.SeedProducts() {
		m[p.SKU] = p
	}
	return &CatalogService{products: m}
}

func (s *CatalogService) List(category string) []infrastructure.Product {
	var out []infrastructure.Product
	for _, p := range s.products {
		if category == "" || p.Category == category {
			out = append(out, p)
		}
	}
	return out
}

func (s *CatalogService) Get(sku string) (infrastructure.Product, bool) {
	p, ok := s.products[sku]
	return p, ok
}

func (s *CatalogService) Search(query string) []infrastructure.Product {
	q := strings.ToLower(query)
	var out []infrastructure.Product
	for _, p := range s.products {
		if strings.Contains(strings.ToLower(p.Name), q) || strings.Contains(strings.ToLower(p.Description), q) {
			out = append(out, p)
		}
	}
	return out
}
