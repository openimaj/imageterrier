class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}
		"/imageTerrierIndex/search/$resultsProcessor**/$id?"{
			controller = "imageTerrierIndex"
			action = "search"
		}

		"/"(view:"/index")
		"500"(view:'/error')
	}
}
