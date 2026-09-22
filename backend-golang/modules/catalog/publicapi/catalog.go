// Package publicapi is the ONLY catalog surface other modules/apps may import.
package publicapi

import (
	"github.com/example/ecommerce-shootout/backend-golang/modules/catalog/infrastructure"
	"github.com/example/ecommerce-shootout/backend-golang/modules/catalog/internal"
)

type Product = infrastructure.Product

type CatalogPublicApi struct{ svc *internal.CatalogService }

func NewCatalogPublicApi() *CatalogPublicApi {
	return &CatalogPublicApi{svc: internal.NewCatalogService()}
}
func (c *CatalogPublicApi) List(category string) []Product     { return c.svc.List(category) }
func (c *CatalogPublicApi) Get(sku string) (Product, bool)     { return c.svc.Get(sku) }
func (c *CatalogPublicApi) Search(query string) []Product      { return c.svc.Search(query) }
